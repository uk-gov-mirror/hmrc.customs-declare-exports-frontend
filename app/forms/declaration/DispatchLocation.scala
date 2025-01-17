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

import forms.DeclarationPage
import forms.Mapping.requiredRadio
import forms.declaration.DispatchLocation.AllowedDispatchLocations
import models.DeclarationType.DeclarationType
import models.viewmodels.TariffContentKey
import play.api.data.{Form, Forms, Mapping}
import play.api.libs.json.{Json, OFormat}
import utils.validators.forms.FieldValidator.isContainedIn

case class DispatchLocation(
  dispatchLocation: String // 2 upper case alphabetic characters
) {

  def isOutsideEU: Boolean = dispatchLocation == AllowedDispatchLocations.OutsideEU
}

object DispatchLocation extends DeclarationPage {
  implicit val format: OFormat[DispatchLocation] = Json.format[DispatchLocation]

  private val allowedValues = Set(AllowedDispatchLocations.OutsideEU, AllowedDispatchLocations.SpecialFiscalTerritory)

  val formMapping: Mapping[DispatchLocation] = Forms.mapping(
    "dispatchLocation" -> requiredRadio("declaration.dispatchLocation.inputText.error.empty")
      .verifying("declaration.dispatchLocation.inputText.error.incorrect", isContainedIn(allowedValues))
  )(DispatchLocation.apply)(DispatchLocation.unapply)

  val formId: String = "DispatchLocation"

  def form(): Form[DispatchLocation] = Form(formMapping)

  object AllowedDispatchLocations {
    val OutsideEU = "EX"
    val SpecialFiscalTerritory = "CO"
  }

  override def defineTariffContentKeys(decType: DeclarationType): Seq[TariffContentKey] =
    Seq(TariffContentKey(s"tariff.declaration.dispatchLocation.${DeclarationPage.getJourneyTypeSpecialisation(decType)}"))
}
