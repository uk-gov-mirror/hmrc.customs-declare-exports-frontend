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

import base.Injector
import config.{AppConfig, SecureMessagingInboxConfig}
import controllers.ChoiceController
import controllers.actions.NoExternalId
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import unit.base.ControllerWithoutFormSpec
import views.html.choice_page

class AuthActionSpec extends ControllerWithoutFormSpec with Injector {

  val choicePage = instanceOf[choice_page]
  val appConfig = mock[AppConfig]
  val secureMessagingInboxConfig = mock[SecureMessagingInboxConfig]

  val controller =
    new ChoiceController(
      mockAuthAction,
      mockVerifiedEmailAction,
      stubMessagesControllerComponents(),
      secureMessagingInboxConfig,
      choicePage,
      appConfig
    )

  "Auth Action" should {

    "return InsufficientEnrolments when EORI number is missing" in {
      userWithoutEori()

      val result = controller.displayPage(None)(getRequest())

      intercept[InsufficientEnrolments](status(result))
    }

    "return NoExternalId when External Id is missing" in {
      userWithoutExternalId()

      val result = controller.displayPage(None)(getRequest())

      intercept[NoExternalId](status(result))
    }
  }
}
