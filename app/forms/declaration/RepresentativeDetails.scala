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

import forms.MetadataPropertiesConvertable
import play.api.data.Forms.{optional, text}
import play.api.data.{Form, Forms}
import play.api.libs.json.Json
import services.Countries.allCountries
import utils.validators.forms.FieldValidator.isContainedIn

case class RepresentativeDetails(
  details: EntityDetails,
  statusCode: String //  numeric, [1] or [2] or [3]
) extends MetadataPropertiesConvertable {

  override def toMetadataProperties(): Map[String, String] =
    Map("declaration.agent.id" -> details.eori.getOrElse(""), "declaration.agent.functionCode" -> statusCode) ++ buildAddressProperties()

  private def buildAddressProperties(): Map[String, String] = details.address match {
    case Some(address) =>
      Map(
        "declaration.agent.name" -> address.fullName,
        "declaration.agent.address.line" -> address.addressLine,
        "declaration.agent.address.cityName" -> address.townOrCity,
        "declaration.agent.address.postcodeId" -> address.postCode,
        "declaration.agent.address.countryCode" ->
          allCountries.find(country => address.country.contains(country.countryName)).map(_.countryCode).getOrElse("")
      )
    case None => Map.empty
  }

}

object RepresentativeDetails {
  implicit val format = Json.format[RepresentativeDetails]

  import StatusCodes._
  private val representativeStatusCodeAllowedValues =
    Set(Declarant, DirectRepresentative, IndirectRepresentative)

  val formId = "RepresentativeDetails"

  val mapping = Forms.mapping(
    "details" -> EntityDetails.mapping,
    "statusCode" -> optional(
      text().verifying(
        "supplementary.representative.representationType.error.wrongValue",
        isContainedIn(representativeStatusCodeAllowedValues)
      )
    ).verifying("supplementary.representative.representationType.error.empty", _.isDefined)
      .transform[String](value => value.getOrElse(""), statusCode => Some(statusCode))
  )(RepresentativeDetails.apply)(RepresentativeDetails.unapply)

  def form(): Form[RepresentativeDetails] = Form(mapping)

  object StatusCodes {
    val Declarant = "1"
    val DirectRepresentative = "2"
    val IndirectRepresentative = "3"
  }
}