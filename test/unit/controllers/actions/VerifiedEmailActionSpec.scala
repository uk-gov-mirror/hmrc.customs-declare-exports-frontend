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

package unit.controllers.actions

import base.{ExportsTestData, Injector}
import connectors.CustomsDeclareExportsConnector
import controllers.actions.VerifiedEmailActionImpl
import models.{EORI, VerifiedEmailAddress}
import models.requests.{AuthenticatedRequest, VerifiedEmailRequest}
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito.{reset, when}
import org.scalatest.concurrent.ScalaFutures
import play.api.{Configuration, Environment}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import unit.base.ControllerWithoutFormSpec

import java.time.ZonedDateTime
import scala.concurrent.Future

class VerifiedEmailActionSpec extends ControllerWithoutFormSpec with Injector with ScalaFutures {

  lazy val conf = instanceOf[Configuration]
  lazy val env = instanceOf[Environment]
  lazy val mcc = instanceOf[MessagesControllerComponents]
  lazy val backendConnector = mock[CustomsDeclareExportsConnector]

  lazy val action = new ActionTestWrapper(backendConnector, mcc)

  lazy val sampleEmailAddress = "example@example.com"
  lazy val sampleEori = EORI("12345")
  lazy val user = ExportsTestData.newUser(sampleEori.value, "Id")

  lazy val verifiedEmail = VerifiedEmailAddress(sampleEmailAddress, ZonedDateTime.now())
  lazy val authenticatedRequest = new AuthenticatedRequest[Any](FakeRequest("GET", "requestPath"), user)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(backendConnector)
  }

  "VerifiedEmailAction" should {
    "return a VerifiedEmailRequest" when {
      "user has a verified email address" in {
        when(backendConnector.getVerifiedEmailAddress(meq(sampleEori))(any(), any())).thenReturn(Future.successful(Some(verifiedEmail)))

        val request = new AuthenticatedRequest(authenticatedRequest, user)

        whenReady(action.testRefine(request)) { result =>
          result mustBe Right(VerifiedEmailRequest(request, sampleEmailAddress))
        }
      }
    }

    "return a redirection Result" when {
      "user has no verified email address" in {
        when(backendConnector.getVerifiedEmailAddress(meq(sampleEori))(any(), any())).thenReturn(Future.successful(None))

        val request = new AuthenticatedRequest(authenticatedRequest, user)

        whenReady(action.testRefine(request)) { result =>
          result must be('left)
        }
      }
    }

    "propagate exception" when {
      "connector fails" in {
        when(backendConnector.getVerifiedEmailAddress(meq(sampleEori))(any(), any()))
          .thenReturn(Future.failed(new Exception("Some unhappy response")))

        val request = new AuthenticatedRequest(authenticatedRequest, user)
        val result = action.testRefine(request)

        assert(result.failed.futureValue.isInstanceOf[Exception])
      }
    }
  }

  class ActionTestWrapper(backendConnector: CustomsDeclareExportsConnector, mcc: MessagesControllerComponents)
      extends VerifiedEmailActionImpl(backendConnector, mcc) {
    def testRefine[A](request: AuthenticatedRequest[A]): Future[Either[Result, VerifiedEmailRequest[A]]] =
      refine(request)
  }
}
