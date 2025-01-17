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
import models.DeclarationType.{CLEARANCE, DeclarationType}
import models.viewmodels.TariffContentKey
import play.api.data.{Form, Forms}
import play.api.data.Forms.{optional, text}
import play.api.libs.json.Json
import utils.validators.forms.FieldValidator._

case class ProcedureCodes(
  procedureCode: Option[String], // max 4 alphanumeric characters
  additionalProcedureCode: Option[String] // max 99 codes, each is max 3 alphanumeric characters
) {
  def extractProcedureCode(): (Option[String], Option[String]) =
    (procedureCode.map(_.substring(0, 2)), procedureCode.map(_.substring(2, 4)))
}

object ProcedureCodes extends DeclarationPage {
  implicit val format = Json.format[ProcedureCodes]

  private val procedureCodeLength = 4
  private val additionalProcedureCodeLength = 3

  val mapping = Forms.mapping(
    "procedureCode" -> optional(
      text()
        .verifying("declaration.procedureCodes.procedureCode.error.empty", _.trim.nonEmpty)
        .verifying("declaration.procedureCodes.procedureCode.error.invalid", isEmpty or (hasSpecificLength(procedureCodeLength) and isAlphanumeric))
    ),
    "additionalProcedureCode" -> optional(
      text()
        .verifying(
          "declaration.procedureCodes.additionalProcedureCode.error.invalid",
          isEmpty or (hasSpecificLength(additionalProcedureCodeLength) and isAlphanumeric)
        )
    )
  )(ProcedureCodes.apply)(ProcedureCodes.unapply)

  def form(): Form[ProcedureCodes] = Form(mapping)

  override def defineTariffContentKeys(decType: DeclarationType): Seq[TariffContentKey] =
    decType match {
      case CLEARANCE =>
        Seq(
          TariffContentKey("tariff.declaration.item.procedureCodes.1.clearance"),
          TariffContentKey("tariff.declaration.item.procedureCodes.2.clearance"),
          TariffContentKey("tariff.declaration.item.procedureCodes.3.clearance")
        )
      case _ =>
        Seq(TariffContentKey("tariff.declaration.item.procedureCodes.1.common"), TariffContentKey("tariff.declaration.item.procedureCodes.2.common"))
    }
}
