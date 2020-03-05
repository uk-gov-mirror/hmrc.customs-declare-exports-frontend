/*
 * Copyright 2020 HM Revenue & Customs
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

package views

import java.time.LocalDateTime

import base.Injector
import controllers.routes
import forms.Choice
import forms.Choice.AllowedChoiceValues.Submissions
import models.declaration.notifications.Notification
import models.declaration.submissions.RequestType.{CancellationRequest, SubmissionRequest}
import models.declaration.submissions.{Action, Submission, SubmissionStatus}
import org.jsoup.nodes.Element
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.Helpers.stubMessages
import play.twirl.api.Html
import services.cache.ExportsTestData
import unit.tools.Stubs
import views.declaration.spec.UnitViewSpec
import views.html.submissions
import views.tags.ViewTest

@ViewTest
class SubmissionsViewSpec extends UnitViewSpec with ExportsTestData with Stubs with Injector {

  private val page = new submissions(mainTemplate)
  private def createView(data: Seq[(Submission, Seq[Notification])] = Seq.empty, messages: Messages = stubMessages()): Html =
    page(data)(request, messages)

  "Submission View" should {

    "have proper messages for labels" in {
      val messages = instanceOf[MessagesApi].preferred(journeyRequest())
      messages must haveTranslationFor("submissions.title")
      messages must haveTranslationFor("site.backToSelectionPage")
      messages must haveTranslationFor("submissions.ucr.header")
      messages must haveTranslationFor("submissions.lrn.header")
      messages must haveTranslationFor("submissions.mrn.header")
      messages must haveTranslationFor("submissions.dateAndTime.header")
      messages must haveTranslationFor("submissions.status.header")
    }

    val view = createView()

    "display same page title as header" in {
      val viewWithMessage = createView(messages = realMessagesApi.preferred(request))
      viewWithMessage.title() must include(viewWithMessage.getElementsByTag("h1").text())
    }

    "display page messages" in {
      tableCell(view)(0, 0).text() mustBe "submissions.ucr.header"
      tableCell(view)(0, 1).text() mustBe "submissions.lrn.header"
      tableCell(view)(0, 2).text() mustBe "submissions.mrn.header"
      tableCell(view)(0, 3).text() mustBe "submissions.dateAndTime.header"
      tableCell(view)(0, 4).text() mustBe "submissions.status.header"
    }

    "display page submissions" when {
      val actionSubmission = Action(requestType = SubmissionRequest, id = "conv-id", requestTimestamp = LocalDateTime.of(2019, 1, 1, 0, 0, 0))

      val actionCancellation = Action(requestType = CancellationRequest, id = "conv-id", requestTimestamp = LocalDateTime.of(2021, 1, 1, 0, 0, 0))

      val submission = Submission(
        uuid = "id",
        eori = "eori",
        lrn = "lrn",
        mrn = Some("mrn"),
        ducr = Some("ducr"),
        actions = Seq(actionSubmission, actionCancellation)
      )

      val notification = Notification(
        actionId = "action-id",
        mrn = "mrn",
        dateTimeIssued = LocalDateTime.of(2020, 1, 1, 0, 0, 0),
        status = SubmissionStatus.ACCEPTED,
        errors = Seq.empty,
        payload = "payload"
      )

      val rejectedNotification = Notification(
        actionId = "actionId",
        mrn = "mrn",
        dateTimeIssued = LocalDateTime.now(),
        status = SubmissionStatus.REJECTED,
        errors = Seq.empty,
        payload = ""
      )

      "all fields are populated" in {
        val view = createView(Seq(submission -> Seq(notification)))

        tableCell(view)(1, 0).text() mustBe "ducr"
        tableCell(view)(1, 1).text() mustBe "lrn"
        tableCell(view)(1, 2).text() mustBe "mrn"
        tableCell(view)(1, 3).text() mustBe "1 January 2019 at 00:00"
        tableCell(view)(1, 4).text() mustBe "Accepted"
        val decInformationLink = tableCell(view)(1, 0).getElementsByTag("a").first()
        decInformationLink.attr("href") mustBe routes.SubmissionsController.displayDeclarationWithNotifications("id").url
      }

      "optional fields are unpopulated" in {
        val submissionWithOptionalFieldsEmpty = submission.copy(ducr = None, mrn = None)
        val view = createView(Seq(submissionWithOptionalFieldsEmpty -> Seq(notification)))

        tableCell(view)(1, 0).text() mustBe empty
        tableCell(view)(1, 1).text() mustBe "lrn"
        tableCell(view)(1, 2).text() mustBe empty
        tableCell(view)(1, 3).text() mustBe "1 January 2019 at 00:00"
        tableCell(view)(1, 4).text() mustBe "Accepted"
        val decInformationLink = tableCell(view)(1, 0).getElementsByTag("a").first()
        decInformationLink.attr("href") mustBe routes.SubmissionsController.displayDeclarationWithNotifications("id").url
      }

      "submission status is 'pending' due to missing notification" in {
        val view = createView(Seq(submission -> Seq.empty))

        tableCell(view)(1, 4).text() mustBe "Pending"
      }

      "submission has link when contains rejected notification" in {
        val view = createView(Seq(submission -> Seq(rejectedNotification)))

        tableCell(view)(1, 0).text() mustBe submission.ducr.get
        tableCell(view)(1, 0).toString must include(routes.SubmissionsController.displayDeclarationWithNotifications(submission.uuid).url)
      }

      "submission date is unknown due to missing submit action" in {
        val submissionWithMissingSubmitAction = submission.copy(actions = Seq(actionCancellation))
        val view = createView(Seq(submissionWithMissingSubmitAction -> Seq(notification)))

        tableCell(view)(1, 3).text() mustBe empty
      }
    }

    "display 'Back' button that links to 'Choice' page with Submissions selected" in {
      val backButton = view.getElementById("back-link")

      backButton must containText("site.back")
      backButton must haveHref(routes.ChoiceController.displayPage(Some(Choice(Submissions))))
    }

    "display 'Start a new declaration' link on page" in {
      val startButton = view.select(".button")
      startButton.text() mustBe "supplementary.startNewDec"
      startButton.attr("href") mustBe routes.ChoiceController.displayPage().url
    }
  }

  private def tableCell(view: Html)(row: Int, column: Int): Element =
    view
      .select(".table-row")
      .get(row)
      .getElementsByClass("table-cell")
      .get(column)
}
