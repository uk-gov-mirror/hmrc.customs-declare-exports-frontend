/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package unit.controllers

import java.time.{Instant, LocalDate, ZoneOffset, ZonedDateTime}
import java.util.UUID

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.util.Timeout
import config.PaginationConfig
import connectors.exchange.ExportsDeclarationExchange
import controllers.SubmissionsController
import models._
import models.declaration.notifications.Notification
import models.declaration.submissions.RequestType.SubmissionRequest
import models.declaration.submissions.{Action, Submission, SubmissionStatus}
import models.requests.ExportsSessionKeys
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.{BeMatcher, MatchResult}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import unit.base.ControllerWithoutFormSpec
import views.html.{declaration_information, submissions}

class SubmissionsControllerSpec extends ControllerWithoutFormSpec with BeforeAndAfterEach {

  private val notification =
    Notification("conversationID", "mrn", ZonedDateTime.now(ZoneOffset.UTC), SubmissionStatus.UNKNOWN, Seq.empty)
  private val submission = Submission(
    uuid = UUID.randomUUID().toString,
    eori = "eori",
    lrn = "lrn",
    mrn = None,
    ducr = None,
    actions = Seq(Action(requestType = SubmissionRequest, id = "conversationID", requestTimestamp = ZonedDateTime.now(ZoneOffset.UTC)))
  )
  private val submissionsPage = mock[submissions]
  private val declarationInformationPage = mock[declaration_information]
  private val paginationConfig = mock[PaginationConfig]

  val controller = new SubmissionsController(
    mockAuthAction,
    mockVerifiedEmailAction,
    mockCustomsDeclareExportsConnector,
    stubMessagesControllerComponents(),
    submissionsPage,
    declarationInformationPage
  )(ec, paginationConfig)

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    authorizedUser()
    when(declarationInformationPage.apply(any(), any())(any(), any())).thenReturn(HtmlFormat.empty)
    when(submissionsPage.apply(any())(any(), any())).thenReturn(HtmlFormat.empty)
    when(paginationConfig.itemsPerPage).thenReturn(Page.DEFAULT_MAX_SIZE)
  }

  override protected def afterEach(): Unit =
    reset(declarationInformationPage, mockCustomsDeclareExportsConnector, submissionsPage, paginationConfig)

  def submissionsPagesElementsCaptor: SubmissionsPagesElements = {
    val captor = ArgumentCaptor.forClass(classOf[SubmissionsPagesElements])
    verify(submissionsPage).apply(captor.capture())(any(), any())
    captor.getValue
  }

  def declarationInformationPageCaptor: (Submission, Seq[Notification]) = {
    val submissionCaptor = ArgumentCaptor.forClass(classOf[Submission])
    val notificationsCaptor = ArgumentCaptor.forClass(classOf[Seq[Notification]])
    verify(declarationInformationPage).apply(submissionCaptor.capture(), notificationsCaptor.capture())(any(), any())
    (submissionCaptor.getValue, notificationsCaptor.getValue)
  }

  "Display Submissions" should {

    "return 200 (OK)" when {

      "display list of submissions method is invoked" in {

        when(mockCustomsDeclareExportsConnector.fetchSubmissions()(any(), any()))
          .thenReturn(Future.successful(Seq(submission)))
        when(mockCustomsDeclareExportsConnector.fetchNotifications()(any(), any()))
          .thenReturn(Future.successful(Seq(notification)))

        val result = controller.displayListOfSubmissions()(getRequest())

        val expectedOtherSubmissionsPassed = Paginated(Seq((submission, Seq(notification))), Page(), 1)

        status(result) mustBe OK
        submissionsPagesElementsCaptor.otherSubmissions mustBe expectedOtherSubmissionsPassed
      }

      "display declaration with notification method is invoked" in {

        when(mockCustomsDeclareExportsConnector.findSubmission(any())(any(), any()))
          .thenReturn(Future.successful(Some(submission)))
        when(mockCustomsDeclareExportsConnector.findNotifications(any())(any(), any()))
          .thenReturn(Future.successful(Seq(notification)))

        val result = controller.displayDeclarationWithNotifications("conversationID")(getRequest())

        val expectedArguments = (submission, Seq(notification))

        status(result) mustBe OK
        declarationInformationPageCaptor mustBe expectedArguments
      }
    }

    "return 303 (SEE_OTHER)" when {

      "there is no submission during display Declaration with notification method" in {

        when(mockCustomsDeclareExportsConnector.findSubmission(any())(any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.displayDeclarationWithNotifications("conversationID")(getRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.routes.SubmissionsController.displayListOfSubmissions().url
      }

      "viewing a declaration" in {
        val result = controller.viewDeclaration("some-id")(getRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.declaration.routes.SubmittedDeclarationController.displayPage().url
        session(result).get(ExportsSessionKeys.declarationId) mustBe Some("some-id")
      }
    }
  }

  def inTheLast(timeout: Timeout): BeMatcher[Instant] = new BeMatcher[Instant] {
    override def apply(left: Instant): MatchResult = {
      val currentTime = Instant.now()
      MatchResult(
        left != null && left.plusSeconds(timeout.duration.toSeconds).isAfter(currentTime),
        s"Instant was ${currentTime.getEpochSecond - left.getEpochSecond} seconds ago",
        s"Instant was ${currentTime.getEpochSecond - left.getEpochSecond} seconds ago, expected it to be later"
      )
    }
  }

  def theDeclarationCreated: ExportsDeclarationExchange = {
    val captor: ArgumentCaptor[ExportsDeclarationExchange] = ArgumentCaptor.forClass(classOf[ExportsDeclarationExchange])
    verify(mockCustomsDeclareExportsConnector).createDeclaration(captor.capture())(any(), any())
    captor.getValue
  }

  "Amend Declaration" should {

    "return 303 (SEE OTHER)" when {

      "declaration found without declaration in progress" in {

        val rejectedDeclaration: ExportsDeclaration =
          aDeclaration(withId("id"), withStatus(DeclarationStatus.COMPLETE), withUpdateDate(LocalDate.MIN), withCreatedDate(LocalDate.MIN))
        val newDeclaration: ExportsDeclaration = aDeclaration(withId("new-id"), withStatus(DeclarationStatus.DRAFT))

        when(mockCustomsDeclareExportsConnector.findDeclaration(refEq("id"))(any(), any()))
          .thenReturn(Future.successful(Some(rejectedDeclaration)))
        when(mockCustomsDeclareExportsConnector.createDeclaration(any[ExportsDeclarationExchange])(any(), any()))
          .thenReturn(Future.successful(newDeclaration))

        val result = controller.amend("id")(getRequest(None))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.declaration.routes.SummaryController.displayPage(Mode.Amend).url)
        session(result).get(ExportsSessionKeys.declarationId) mustBe Some("new-id")

        val created = theDeclarationCreated

        created.status mustBe DeclarationStatus.DRAFT
        created.sourceId mustBe Some("id")
        created.id mustBe None
        created.updatedDateTime mustBe inTheLast(1 seconds)
        created.createdDateTime mustBe inTheLast(1 seconds)
      }

      "there is a declaration in progress" in {

        val decId = UUID.randomUUID().toString
        val declaration: ExportsDeclaration = aDeclaration(withId(decId), withStatus(DeclarationStatus.DRAFT))

        when(mockCustomsDeclareExportsConnector.findDeclaration(refEq(decId))(any(), any()))
          .thenReturn(Future.successful(Some(declaration)))

        val result = controller.amend(decId)(getRequest(Some(decId)))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe controllers.declaration.routes.SummaryController.displayPage(Mode.Amend).url
      }

      "declaration not found" in {

        when(mockCustomsDeclareExportsConnector.findDeclaration(refEq("id"))(any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.amend("id")(getRequest(None))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.SubmissionsController.displayListOfSubmissions().url)
      }
    }
  }

  "Amend Errors" should {

    "return 303 (SEE OTHER)" when {

      "declaration found without declaration in progress" in {

        val redirectUrl = "/specific-page-url"
        val rejectedDeclaration: ExportsDeclaration =
          aDeclaration(withId("id"), withStatus(DeclarationStatus.COMPLETE), withUpdateDate(LocalDate.MIN), withCreatedDate(LocalDate.MIN))
        val newDeclaration: ExportsDeclaration = aDeclaration(withId("new-id"), withStatus(DeclarationStatus.DRAFT))

        when(mockCustomsDeclareExportsConnector.findDeclaration(refEq("id"))(any(), any()))
          .thenReturn(Future.successful(Some(rejectedDeclaration)))
        when(mockCustomsDeclareExportsConnector.createDeclaration(any[ExportsDeclarationExchange])(any(), any()))
          .thenReturn(Future.successful(newDeclaration))

        val result = controller.amendErrors("id", redirectUrl, "pattern", "message")(getRequest(None))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe redirectUrl + "?mode=Error-Fix"
        session(result).get(ExportsSessionKeys.declarationId) mustBe Some("new-id")

        val created = theDeclarationCreated

        created.status mustBe DeclarationStatus.DRAFT
        created.sourceId mustBe Some("id")
        created.id mustBe None
        created.updatedDateTime mustBe inTheLast(1 seconds)
        created.createdDateTime mustBe inTheLast(1 seconds)
      }

      "there is a declaration in progress with the same source Id" in {

        val redirectUrl = "/specific-page-url"
        val sourceId = UUID.randomUUID().toString
        val decId = UUID.randomUUID().toString
        val declaration: ExportsDeclaration = aDeclaration(withId(decId), withSourceId(sourceId), withStatus(DeclarationStatus.DRAFT))

        when(mockCustomsDeclareExportsConnector.findDeclaration(refEq(decId))(any(), any()))
          .thenReturn(Future.successful(Some(declaration)))

        val result = controller.amendErrors(sourceId, redirectUrl, "pattern", "message")(getRequest(Some(decId)))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe redirectUrl + "?mode=Error-Fix"
      }

      "there is declaration in progress without sourceId" in {

        val redirectUrl = "/specific-page-url"
        val rejDecId = UUID.randomUUID().toString
        val actualDecId = UUID.randomUUID().toString
        val newDecId = UUID.randomUUID().toString
        val rejectedDeclaration: ExportsDeclaration =
          aDeclaration(withId(rejDecId), withStatus(DeclarationStatus.COMPLETE), withUpdateDate(LocalDate.MIN), withCreatedDate(LocalDate.MIN))
        val actualDeclaration: ExportsDeclaration = aDeclaration(withId(actualDecId), withoutSourceId())
        val newDeclaration: ExportsDeclaration = aDeclaration(withId(newDecId), withStatus(DeclarationStatus.DRAFT))

        when(mockCustomsDeclareExportsConnector.findDeclaration(refEq(rejDecId))(any(), any()))
          .thenReturn(Future.successful(Some(rejectedDeclaration)))
        when(mockCustomsDeclareExportsConnector.findDeclaration(refEq(actualDecId))(any(), any()))
          .thenReturn(Future.successful(Some(actualDeclaration)))
        when(mockCustomsDeclareExportsConnector.createDeclaration(any[ExportsDeclarationExchange])(any(), any()))
          .thenReturn(Future.successful(newDeclaration))

        val result = controller.amendErrors(rejDecId, redirectUrl, "pattern", "message")(getRequest(Some(actualDecId)))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get mustBe redirectUrl + "?mode=Error-Fix"
        session(result).get(ExportsSessionKeys.declarationId) mustBe Some(newDecId)

        val created = theDeclarationCreated

        created.status mustBe DeclarationStatus.DRAFT
        created.sourceId mustBe Some(rejDecId)
        created.id mustBe None
        created.updatedDateTime mustBe inTheLast(1 seconds)
        created.createdDateTime mustBe inTheLast(1 seconds)
      }

      "declaration not found" in {

        val sourceId = UUID.randomUUID().toString

        when(mockCustomsDeclareExportsConnector.findDeclaration(refEq(sourceId))(any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.amendErrors(sourceId, "redirectUrl", "pattern", "message")(getRequest(None))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.SubmissionsController.displayListOfSubmissions().url)
      }
    }
  }
}
