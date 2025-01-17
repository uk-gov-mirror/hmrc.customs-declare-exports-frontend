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
import models.viewmodels.TariffContentKey
import play.api.data.FormError

class ProcedureCodesSpec extends DeclarationPageBaseSpec {
  import ProcedureCodesSpec._

  "Procedure Code form" should {

    "return form with errors" when {

      "procedure code is incorrect" in {
        val form = ProcedureCodes.form().bind(procedureCodeIncorrectLength)

        form.errors mustBe Seq(FormError("procedureCode", "declaration.procedureCodes.procedureCode.error.invalid"))
      }

      "procedure code has special chars" in {
        val form = ProcedureCodes.form().bind(procedureCodeSpecialChars)

        form.errors mustBe Seq(FormError("procedureCode", "declaration.procedureCodes.procedureCode.error.invalid"))
      }

      "procedure code incorrect with special chars" in {
        val form = ProcedureCodes.form().bind(procedureCodeIncorrectLengthSpecialChars)

        form.errors mustBe Seq(FormError("procedureCode", "declaration.procedureCodes.procedureCode.error.invalid"))
      }

      "additional code has incorrect length" in {
        val form = ProcedureCodes.form().bind(tooLongAdditionalProcedureCode)

        form.errors.length must be(1)
        form.errors.head must be(FormError("additionalProcedureCode", "declaration.procedureCodes.additionalProcedureCode.error.invalid"))
      }

      "additional code has special characters" in {

        val form = ProcedureCodes.form().bind(additionalProcedureCodeSpecialChars)

        form.errors.length must be(1)
        form.errors.head must be(FormError("additionalProcedureCode", "declaration.procedureCodes.additionalProcedureCode.error.invalid"))
      }

      "additional code has incorrect length with special characters" in {
        val form = ProcedureCodes.form().bind(tooLongAdditionalProcedureCodeWithSpecialCharacters)

        form.errors.length must be(1)
        form.errors.head must be(FormError("additionalProcedureCode", "declaration.procedureCodes.additionalProcedureCode.error.invalid"))
      }
    }
  }

  override def getCommonTariffKeys(messageKey: String): Seq[TariffContentKey] =
    Seq(TariffContentKey(s"${messageKey}.1.common"), TariffContentKey(s"${messageKey}.2.common"))

  override def getClearanceTariffKeys(messageKey: String): Seq[TariffContentKey] =
    Seq(
      TariffContentKey(s"${messageKey}.1.clearance"),
      TariffContentKey(s"${messageKey}.2.clearance"),
      TariffContentKey(s"${messageKey}.3.clearance")
    )

  "ProcedureCodes" when {
    testTariffContentKeys(ProcedureCodes, "tariff.declaration.item.procedureCodes")
  }
}

object ProcedureCodesSpec {

  val procedureCodeIncorrectLength: Map[String, String] = Map("procedureCode" -> "21", "additionalProcedureCode" -> "123")
  val procedureCodeSpecialChars: Map[String, String] = Map("procedureCode" -> "21##", "additionalProcedureCode" -> "123")
  val procedureCodeIncorrectLengthSpecialChars: Map[String, String] = Map("procedureCode" -> "12321##", "additionalProcedureCode" -> "123")

  val tooLongAdditionalProcedureCode: Map[String, String] =
    Map("procedureCode" -> "2112", "additionalProcedureCode" -> "123456")

  val additionalProcedureCodeSpecialChars: Map[String, String] =
    Map("procedureCode" -> "2112", "additionalProcedureCode" -> "#$%")

  val tooLongAdditionalProcedureCodeWithSpecialCharacters: Map[String, String] =
    Map("procedureCode" -> "2112", "additionalProcedureCode" -> "123456#$")
}
