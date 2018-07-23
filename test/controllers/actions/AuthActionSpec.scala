/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.actions

import play.api.mvc.Controller
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import base.SpecBase
import controllers.routes
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class AuthActionSpec extends SpecBase {

  class Harness(authAction: AuthAction) extends Controller {
    def onPageLoad() = authAction { _ => Ok }
  }

  "Auth Action" when {
    "the user hasn't logged in" must {
      "redirect the user to log in " in {
        val fakeFailingAuthConnector = new FakeFailingAuthConnector(new MissingBearerToken)
        val authAction = new AuthActionImpl(fakeFailingAuthConnector, frontendAppConfig)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must startWith(frontendAppConfig.loginUrl)
      }
    }

    "the user's session has expired" must {
      "redirect the user to log in " in {
        val fakeFailingAuthConnector = new FakeFailingAuthConnector(new BearerTokenExpired)
        val authAction = new AuthActionImpl(fakeFailingAuthConnector, frontendAppConfig)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must startWith(frontendAppConfig.loginUrl)
      }
    }

    "the user doesn't have sufficient enrolments" must {
      "redirect the user to the unauthorised page" in {
        val fakeFailingAuthConnector = new FakeFailingAuthConnector(new InsufficientEnrolments)
        val authAction = new AuthActionImpl(fakeFailingAuthConnector, frontendAppConfig)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
      }
    }

    "the user doesn't have sufficient confidence level" must {
      "redirect the user to the unauthorised page" in {
        val fakeFailingAuthConnector = new FakeFailingAuthConnector(new InsufficientConfidenceLevel)
        val authAction = new AuthActionImpl(fakeFailingAuthConnector, frontendAppConfig)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
      }
    }

    "the user used an unaccepted auth provider" must {
      "redirect the user to the unauthorised page" in {
        val fakeFailingAuthConnector = new FakeFailingAuthConnector(new UnsupportedAuthProvider)
        val authAction = new AuthActionImpl(fakeFailingAuthConnector, frontendAppConfig)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
      }
    }

    "the user has an unsupported affinity group" must {
      "redirect the user to the unauthorised page" in {
        val fakeFailingAuthConnector = new FakeFailingAuthConnector(new UnsupportedAffinityGroup)
        val authAction = new AuthActionImpl(fakeFailingAuthConnector, frontendAppConfig)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
      }
    }

    "the user has an unsupported credential role" must {
      "redirect the user to the unauthorised page" in {
        val fakeFailingAuthConnector = new FakeFailingAuthConnector(new UnsupportedCredentialRole)
        val authAction = new AuthActionImpl(fakeFailingAuthConnector, frontendAppConfig)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
      }
    }
  }
}

class FakeFailingAuthConnector(exceptionToReturn: Throwable) extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](
    predicate: Predicate,
    retrieval: Retrieval[A]
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = Future.failed(exceptionToReturn)
}


