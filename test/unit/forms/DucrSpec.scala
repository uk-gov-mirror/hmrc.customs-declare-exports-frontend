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

package unit.forms

import forms.Ducr
import play.api.data.{Form, FormError}
import unit.base.UnitSpec

class DucrSpec extends UnitSpec {

  "Ducr" should {

    "correctly convert DUCR to upper case characters" in {

      Ducr.form2Data("9gb123456664559-1abc") mustBe Ducr("9GB123456664559-1ABC")
    }
  }

  "Ducr mapping" should {

    "return error for incorrect DUCR" in {

      val incorrectDucr = Ducr("91B123456664559-654A")
      val filledForm = Form(Ducr.ducrMapping).fillAndValidate(incorrectDucr)

      val expectedError = FormError("ducr", "error.ducr")

      filledForm.errors mustBe Seq(expectedError)
    }

    "has no errors for correct Ducr" in {

      val correctDucr = Ducr("9GB123456664559-1H7-1")
      val filledForm = Form(Ducr.ducrMapping).fillAndValidate(correctDucr)

      filledForm.errors mustBe empty
    }
  }
}
