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

package connectors

import java.time.{ZoneOffset, ZonedDateTime}
import java.util.UUID

import base.TestHelper._
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.exchange.ExportsDeclarationExchange
import forms.{CancelDeclaration, Lrn}
import models.declaration.notifications.Notification
import models.declaration.submissions.RequestType.SubmissionRequest
import models.declaration.submissions.{Action, Submission, SubmissionStatus}
import models.{Page, Paginated}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.http.Status
import play.api.libs.json.{Json, Writes}
import play.api.test.Helpers._
import services.cache.ExportsDeclarationBuilder

class CustomsDeclareExportsConnectorIntegrationSpec extends ConnectorSpec with BeforeAndAfterEach with ExportsDeclarationBuilder with ScalaFutures {

  private val id = "id"
  private val existingDeclaration = aDeclaration(withId(id))
  private val newDeclarationExchange = ExportsDeclarationExchange.withoutId(aDeclaration())
  private val existingDeclarationExchange = ExportsDeclarationExchange(existingDeclaration)
  private val action = Action(UUID.randomUUID().toString, SubmissionRequest)
  private val submission = Submission(id, "eori", "lrn", Some("mrn"), None, Seq(action))
  private val notification = Notification("action-id", "mrn", ZonedDateTime.now(ZoneOffset.UTC), SubmissionStatus.UNKNOWN, Seq.empty)
  private val connector = app.injector.instanceOf[CustomsDeclareExportsConnector]

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    exportsWireMockServer.start()
    WireMock.configureFor(wireHost, exportsWirePort)
  }

  override protected def afterAll(): Unit = {
    exportsWireMockServer.stop()
    super.afterAll()
  }

  "Create Declaration" should {
    "return payload" in {
      stubForExports(
        post("/declarations")
          .willReturn(
            aResponse()
              .withStatus(Status.ACCEPTED)
              .withBody(json(existingDeclarationExchange))
          )
      )

      val response = await(connector.createDeclaration(newDeclarationExchange))

      response mustBe existingDeclaration
      verify(
        postRequestedFor(urlEqualTo("/declarations"))
          .withRequestBody(containing(json(newDeclarationExchange)))
      )
    }
  }

  "Update Declaration" should {
    "return payload" in {
      stubForExports(
        put(s"/declarations/$id")
          .willReturn(
            aResponse()
              .withStatus(Status.ACCEPTED)
              .withBody(json(existingDeclarationExchange))
          )
      )

      val response = await(connector.updateDeclaration(existingDeclaration))

      response mustBe existingDeclaration
      verify(
        putRequestedFor(urlEqualTo(s"/declarations/id"))
          .withRequestBody(containing(json(existingDeclarationExchange)))
      )
    }
  }

  "Submit declaration" should {
    "return submission object" in {
      val payload =
        s"""
           |{
           |  "uuid": "$id",
           |  "eori": "${createRandomAlphanumericString(11)}",
           |  "lrn":  "${createRandomAlphanumericString(8)}",
           |  "actions" : [{
           |      "id" : "${UUID.randomUUID().toString}",
           |      "requestType" : "SubmissionRequest",
           |      "requestTimestamp" : "${ZonedDateTime.now(ZoneOffset.UTC).toString}"
           |   }]
          |}
        """.stripMargin
      stubForExports(
        post(s"/declarations/$id/submission")
          .willReturn(
            aResponse()
              .withStatus(Status.CREATED)
              .withBody(payload)
          )
      )
      val response = await(connector.submitDeclaration(id))

      response.uuid mustBe id
      response.actions must not be empty

      verify(
        postRequestedFor(urlEqualTo(s"/declarations/id/submission"))
          .withRequestBody(absent())
      )
    }
  }

  "Delete Declaration" should {
    "return payload" in {
      stubForExports(
        delete(s"/declarations/$id")
          .willReturn(
            aResponse()
              .withStatus(Status.NO_CONTENT)
          )
      )

      val response = await(connector.deleteDraftDeclaration(id))

      response mustBe ((): Unit)
      verify(deleteRequestedFor(urlEqualTo(s"/declarations/id")))
    }
  }

  "Find Declarations" should {
    val pagination = Page(1, 10)

    "return Ok" in {
      stubForExports(
        get("/declarations?page-index=1&page-size=10")
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withBody(json(Paginated(Seq(existingDeclarationExchange), pagination, 1)))
          )
      )

      val response = await(connector.findDeclarations(pagination))

      response mustBe Paginated(Seq(existingDeclaration), pagination, 1)
      verify(getRequestedFor(urlEqualTo("/declarations?page-index=1&page-size=10")))
    }
  }

  "Find Saved Draft Declarations" should {
    val pagination = Page(1, 10)

    "return Ok" in {
      stubForExports(
        get("/declarations?status=DRAFT&page-index=1&page-size=10&sort-by=updatedDateTime&sort-direction=des")
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withBody(json(Paginated(Seq(existingDeclarationExchange), pagination, 1)))
          )
      )

      val response = await(connector.findSavedDeclarations(pagination))

      response mustBe Paginated(Seq(existingDeclaration), pagination, 1)
      verify(getRequestedFor(urlEqualTo("/declarations?status=DRAFT&page-index=1&page-size=10&sort-by=updatedDateTime&sort-direction=des")))
    }
  }

  "Find Declaration" should {
    "return Ok" in {
      stubForExports(
        get(s"/declarations/$id")
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withBody(json(existingDeclarationExchange))
          )
      )

      val response = await(connector.findDeclaration(id))

      response mustBe Some(existingDeclaration)
      verify(getRequestedFor(urlEqualTo(s"/declarations/$id")))
    }
  }

  "Find Submission" should {
    "return Ok" in {
      stubForExports(
        get(s"/declarations/$id/submission")
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withBody(json(submission))
          )
      )

      val response = await(connector.findSubmission(id))

      response mustBe Some(submission)
      verify(getRequestedFor(urlEqualTo(s"/declarations/$id/submission")))
    }
  }

  "Find Notifications" should {
    "return Ok" in {
      stubForExports(
        get(s"/declarations/$id/submission/notifications")
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withBody(json(Seq(notification)))
          )
      )

      val response = await(connector.findNotifications(id))

      response mustBe Seq(notification)
      verify(getRequestedFor(urlEqualTo(s"/declarations/$id/submission/notifications")))
    }
  }

  "Create Cancellation" should {
    val cancellation = CancelDeclaration(Lrn("ref"), "id", "statement", "reason")

    "return payload" in {
      stubForExports(
        post("/cancellations")
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
          )
      )

      await(connector.createCancellation(cancellation))

      verify(
        postRequestedFor(urlEqualTo("/cancellations"))
          .withRequestBody(containing(json(cancellation)))
      )
    }
  }

  private def json[T](t: T)(implicit wts: Writes[T]): String = Json.toJson(t).toString()

}
