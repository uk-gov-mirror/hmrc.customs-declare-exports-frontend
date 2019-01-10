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

package controllers.supplementary

import base.CustomExportsBaseSpec
import play.api.test.Helpers._

class NotEligibleControllerSpec extends CustomExportsBaseSpec {

  val uri = uriWithContextPath("/declaration/supplementary/not-eligible")

  "NotEligible" should {
    "return 200 with a success" in {
      authorizedUser()

      val result = route(app, getRequest(uri)).get

      status(result) must be(OK)
    }
    "display page content" in {
      authorizedUser()

      val result = route(app, getRequest(uri)).get
      val stringResult = contentAsString(result)

      stringResult must include(messages("notEligible.title"))
      stringResult must include(messages("notEligible.descriptionPreUrl"))
      stringResult must include(messages("notEligible.descriptionUrl"))
      stringResult must include(messages("notEligible.descriptionPostUrl"))
      stringResult must include(messages("notEligible.referenceTitle"))
      stringResult must include(messages("notEligible.reference.text"))
    }
  }
}