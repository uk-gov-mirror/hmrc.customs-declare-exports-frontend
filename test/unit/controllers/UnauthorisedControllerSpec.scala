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

import controllers.UnauthorisedController
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import unit.base.ControllerWithoutFormSpec
import views.html.unauthorised

class UnauthorisedControllerSpec extends ControllerWithoutFormSpec {

  trait SetUp {
    val unauthorisedPage = mock[unauthorised]

    val controller =
      new UnauthorisedController(stubMessagesControllerComponents(), unauthorisedPage)

    when(unauthorisedPage.apply()(any(), any())).thenReturn(HtmlFormat.empty)
  }

  "Unauthorised controller" should {

    "return 200 (OK)" when {

      "display page method is invoked" in new SetUp {

        val result = controller.onPageLoad()(getRequest())

        status(result) must be(OK)
      }
    }
  }
}
