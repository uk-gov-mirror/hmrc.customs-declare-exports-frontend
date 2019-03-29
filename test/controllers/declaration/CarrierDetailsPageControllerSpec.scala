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

package controllers.declaration

import base.CustomExportsBaseSpec
import forms.Choice
import forms.Choice.choiceId
import forms.declaration.CarrierDetails
import forms.declaration.CarrierDetailsSpec._
import play.api.test.Helpers._

class CarrierDetailsPageControllerSpec extends CustomExportsBaseSpec {

  private val uri = uriWithContextPath("/declaration/carrier-details")

  before {
    authorizedUser()
    withCaching[CarrierDetails](None)
    withCaching[Choice](Some(Choice(Choice.AllowedChoiceValues.SupplementaryDec)), choiceId)
  }

  "Carrier Details Page Controller on GET" should {

    "return 200 status code" in {
      val result = route(app, getRequest(uri)).get

      status(result) must be(OK)
    }
  }

  "Carrier Details Page Controller on POST" should {

    "validate request and redirect - both EORI and business details are empty" in {

      val result = route(app, postRequest(uri, emptyCarrierDetailsJSON)).get
      val page = contentAsString(result)

      status(result) must be(BAD_REQUEST)
    }

    "validate request and redirect - only EORI provided" in {

      val result = route(app, postRequest(uri, correctCarrierDetailsEORIOnlyJSON)).get
      val header = result.futureValue.header

      status(result) must be(SEE_OTHER)
      header.headers.get("Location") must be(Some("/customs-declare-exports/declaration/additional-actors"))
    }

    "validate request and redirect - only address provided" in {

      val result = route(app, postRequest(uri, correctCarrierDetailsAddressOnlyJSON)).get
      val header = result.futureValue.header

      status(result) must be(SEE_OTHER)
      header.headers.get("Location") must be(Some("/customs-declare-exports/declaration/additional-actors"))
    }

    "validate request and redirect - all values provided" in {

      val result = route(app, postRequest(uri, correctCarrierDetailsJSON)).get
      val header = result.futureValue.header

      status(result) must be(SEE_OTHER)
      header.headers.get("Location") must be(Some("/customs-declare-exports/declaration/additional-actors"))
    }
  }
}