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

package forms.declaration

import forms.common.DeclarationPageBaseSpec
import play.api.data.FormError
import play.api.libs.json.{JsObject, JsString}

class TransportPaymentSpec extends DeclarationPageBaseSpec {

  def formData(paymentMethod: String) =
    JsObject(Map("paymentMethod" -> JsString(paymentMethod)))

  "TransportContainer mapping" should {

    "return form with errors" when {
      "provided with invalid payment method" in {
        val form = TransportPayment.form().bind(formData("invalid"))

        form.errors mustBe Seq(FormError("paymentMethod", "standard.transportDetails.paymentMethod.error"))
      }
    }

    "return form without errors" when {
      "provided with valid input" in {
        val form = TransportPayment.form().bind(formData(TransportPayment.cash))

        form.hasErrors must be(false)
      }

      "provided with no input for optional question" in {
        val form = TransportPayment.form().bind(formData(""))

        form.hasErrors must be(false)
      }
    }
  }

  "TransportPayment" when {
    testTariffContentKeys(TransportPayment, "tariff.declaration.transportPayment")
  }
}
