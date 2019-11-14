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

package forms.declaration
import forms.DeclarationPage
import forms.Mapping.requiredRadio
import play.api.data.Forms.text
import play.api.data.{Form, Forms}
import play.api.libs.json.Json
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual
import utils.validators.forms.FieldValidator._

case class CUSCode(cusCode: Option[String])

object CUSCode extends DeclarationPage {

  implicit val format = Json.format[CUSCode]

  val hasCusCodeKey = "hasCusCode"
  val cusCodeKey = "cusCode"
  private val cusCodeLength = 8

  object AllowedCUSCodeAnswers {
    val yes = "Yes"
    val no = "No"
  }

  import AllowedCUSCodeAnswers._

  private def form2Model: (String, Option[String]) => CUSCode = {
    case (hasCode, codeValue) =>
      hasCode match {
        case AllowedCUSCodeAnswers.yes => CUSCode(codeValue)
        case AllowedCUSCodeAnswers.no  => CUSCode(None)
      }
  }

  private def model2Form: CUSCode => Option[(String, Option[String])] =
    model =>
      model.cusCode match {
        case Some(code) => Some((yes, Some(code)))
        case None       => Some((no, None))
    }

  val mapping =
    Forms.mapping(
      hasCusCodeKey -> requiredRadio("error.yesNo.required"),
      cusCodeKey -> mandatoryIfEqual(
        hasCusCodeKey,
        yes,
        text()
          .verifying("declaration.cusCode.error.empty", nonEmpty)
          .verifying("declaration.cusCode.error.length", isEmpty or hasSpecificLength(cusCodeLength))
          .verifying("declaration.cusCode.error.specialCharacters", isEmpty or isAlphanumeric)
      )
    )(form2Model)(model2Form)

  def form(): Form[CUSCode] = Form(mapping)
}