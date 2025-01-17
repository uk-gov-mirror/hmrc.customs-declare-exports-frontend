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

import base.TestHelper
import play.api.libs.json._
import unit.base.UnitSpec

class LegalDeclarationSpec extends UnitSpec {

  import LegalDeclaration._

  "Legal Declaration Form" should {
    "not return errors" when {
      "all form data is provided and valid" in {
        form().bind(validFormData).errors mustBe empty
      }
    }

    "return errors for full name" when {
      "name missing" in {
        form().bind(formDataWith(name = "")).errors.map(_.message) must contain("legal.declaration.fullName.empty")
      }
      "name too short" in {
        form().bind(formDataWith(name = "Al")).errors.map(_.message) must contain("legal.declaration.fullName.short")
      }
      "name too long" in {
        form()
          .bind(formDataWith(name = TestHelper.createRandomAlphanumericString(65)))
          .errors
          .map(_.message) must contain("legal.declaration.fullName.long")
      }
      "name invalid" in {
        form().bind(formDataWith(name = "Prince!")).errors.map(_.message) must contain("legal.declaration.fullName.error")
      }
    }

    "return errors for job role" when {
      "job role missing" in {
        form().bind(formDataWith(role = "")).errors.map(_.message) must contain("legal.declaration.jobRole.empty")
      }
      "job role too short" in {
        form().bind(formDataWith(role = "CEO")).errors.map(_.message) must contain("legal.declaration.jobRole.short")
      }
      "job role too long" in {
        form()
          .bind(formDataWith(role = TestHelper.createRandomAlphanumericString(65)))
          .errors
          .map(_.message) must contain("legal.declaration.jobRole.long")
      }
      "job role invalid" in {
        form().bind(formDataWith(role = "Prince!")).errors.map(_.message) must contain("legal.declaration.jobRole.error")
      }
    }

    "return errors for email" when {
      "email missing" in {
        form().bind(formDataWith(email = "")).errors.map(_.message) must contain("legal.declaration.email.empty")
      }
      "email too long" in {
        form()
          .bind(formDataWith(email = TestHelper.createRandomAlphanumericString(65)))
          .errors
          .map(_.message) must contain("legal.declaration.email.long")
      }
      "email invalid" in {
        form().bind(formDataWith(email = "not.an.email.address")).errors.map(_.message) must contain("legal.declaration.email.error")
      }
    }

    "return errors for confirmation" when {
      "not selected" in {
        form().bind(formDataWith(checked = false)).errors.map(_.message) must contain("legal.declaration.confirmation.missing")
      }
    }

  }

  private def validFormData = formDataWith()

  private def formDataWith(
    name: String = "O'Neil Some-Name, Jr.",
    role: String = "Traveling-Secretary for the N.Y. Yankees' Chairman",
    email: String = "some@email.com",
    checked: Boolean = true
  ) = {
    def isChecked = if (checked) JsTrue else JsFalse
    JsObject(Map("fullName" -> JsString(name), "jobRole" -> JsString(role), "email" -> JsString(email), "confirmation" -> isChecked))
  }
}
