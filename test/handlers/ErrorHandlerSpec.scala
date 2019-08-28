/*
 * Copyright 2019 HM Revenue & Customs
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

package handlers

import java.net.URLEncoder

import config.AppConfig
import org.scalatest.OptionValues
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.{InsufficientEnrolments, NoActiveSession}
import unit.base.UnitSpec
import unit.tools.Stubs
import views.html.error_template

import scala.concurrent.Future

class ErrorHandlerSpec extends UnitSpec with Stubs with OptionValues {

  val errorPage = new error_template(govukWrapper)

  val injector = GuiceApplicationBuilder()
    .configure(
      "urls.login" -> "http://localhost:9949/auth-login-stub/gg-sign-in",
      "urls.loginContinue" -> "http://localhost:6791/customs-declare-exports/start"
    )
    .injector()
  val appConfig = injector.instanceOf[AppConfig]
  val request = FakeRequest("GET", "/foo")

  val errorHandler = new ErrorHandler(stubMessagesApi(), errorPage)(appConfig)

  def urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")

  "ErrorHandlerSpec" should {

    "standardErrorTemplate" in {

      val result = errorHandler.standardErrorTemplate("Page Title", "Heading", "Message")(request).body

      result must include("Page Title")
      result must include("Heading")
      result must include("Message")
    }
  }

  "resolve error" should {

    "handle no active session authorisation exception" in {

      val error = new NoActiveSession("A user is not logged in") {}
      val result = Future.successful(errorHandler.resolveError(request, error))
      val expectedLocation =
        s"http://localhost:9949/auth-login-stub/gg-sign-in?continue=${urlEncode("http://localhost:6791/customs-declare-exports/start")}"

      status(result) mustBe Status.SEE_OTHER
      redirectLocation(result) mustBe Some(expectedLocation)
    }

    "handle insufficient enrolments authorisation exception" in {

      val error = InsufficientEnrolments("HMRC-CUS-ORG")
      val result = Future.successful(errorHandler.resolveError(request, error))

      status(result) mustBe Status.SEE_OTHER
      redirectLocation(result).value must endWith("/unauthorised")
    }
  }
}
