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
import forms.mappings.CrossFieldFormatter
import models.DeclarationType
import models.DeclarationType.{CLEARANCE, DeclarationType}
import models.viewmodels.TariffContentKey
import play.api.data.{Form, Forms}
import play.api.data.Forms.{of, optional, text}
import play.api.libs.json.Json
import utils.validators.forms.FieldValidator._

import scala.util.Try

case class CommodityMeasure(supplementaryUnits: Option[String], grossMass: Option[String], netMass: Option[String])

object CommodityMeasure extends DeclarationPage {

  implicit val format = Json.format[CommodityMeasure]

  val commodityFormId = "CommodityMeasure"

  def applyDefault(supplementaryUnits: Option[String], grossMass: String, netMass: String): CommodityMeasure =
    CommodityMeasure(supplementaryUnits, if (grossMass.isEmpty) None else Some(grossMass), if (netMass.isEmpty) None else Some(netMass))

  def unapplyDefault(value: CommodityMeasure): Option[(Option[String], String, String)] =
    Some((value.supplementaryUnits, value.grossMass.getOrElse(""), value.netMass.getOrElse("")))

  def applyClearance(grossMass: Option[String], netMass: Option[String]): CommodityMeasure =
    CommodityMeasure(None, grossMass, netMass)

  def unapplyClearance(value: CommodityMeasure): Option[(Option[String], Option[String])] =
    Some((value.grossMass, value.netMass))

  private val massFormatValidation: String => Boolean = str => validateDecimalGreaterThanZero(16)(6)(str) and containsNotOnlyZeros(str)

  private val mappingDefault = Forms.mapping(
    "supplementaryUnits" -> optional(text().verifying("declaration.commodityMeasure.supplementaryUnits.error", massFormatValidation)),
    "grossMass" -> of(
      CrossFieldFormatter(
        secondaryKey = "",
        constraints = Seq(
          ("declaration.commodityMeasure.grossMass.empty", (gross: String, _: String) => nonEmpty(gross)),
          ("declaration.commodityMeasure.grossMass.error", (gross: String, _: String) => isEmpty(gross) or massFormatValidation(gross))
        )
      )
    ),
    "netMass" -> of(
      CrossFieldFormatter(
        secondaryKey = "grossMass",
        constraints = Seq(
          ("declaration.commodityMeasure.netMass.empty", (net: String, _: String) => nonEmpty(net)),
          ("declaration.commodityMeasure.netMass.error.format", (net: String, _: String) => isEmpty(net) or massFormatValidation(net)),
          (
            "declaration.commodityMeasure.netMass.error.biggerThanGrossMass",
            (net: String, gross: String) => isEmpty(net) or isEmpty(gross) or !massFormatValidation(net) or isFirstSmallerOrEqual(net, gross)
          )
        )
      )
    )
  )(CommodityMeasure.applyDefault)(CommodityMeasure.unapplyDefault)

  private val mappingClearance = Forms.mapping(
    "grossMass" -> optional(
      text()
        .verifying("declaration.commodityMeasure.grossMass.error", isEmpty or massFormatValidation)
    ),
    "netMass" -> optional(
      of(
        CrossFieldFormatter(
          secondaryKey = "grossMass",
          constraints = Seq(
            ("declaration.commodityMeasure.netMass.error.format", (net: String, _: String) => isEmpty(net) or massFormatValidation(net)),
            (
              "declaration.commodityMeasure.netMass.error.biggerThanGrossMass",
              (net: String, gross: String) => isEmpty(net) or isEmpty(gross) or !massFormatValidation(net) or isFirstSmallerOrEqual(net, gross)
            )
          )
        )
      )
    )
  )(CommodityMeasure.applyClearance)(CommodityMeasure.unapplyClearance)

  private def isFirstSmallerOrEqual(first: String, second: String): Boolean =
    Try {
      val firstNum = BigDecimal(first)
      val secondNum = BigDecimal(second)
      firstNum <= secondNum
    }.getOrElse(false)

  def form(declarationType: DeclarationType): Form[CommodityMeasure] = declarationType match {
    case DeclarationType.CLEARANCE => Form(mappingClearance)
    case _                         => Form(mappingDefault)
  }

  override def defineTariffContentKeys(decType: DeclarationType): Seq[TariffContentKey] =
    decType match {
      case CLEARANCE =>
        Seq(
          TariffContentKey("tariff.declaration.item.commodityMeasure.1.clearance"),
          TariffContentKey("tariff.declaration.item.commodityMeasure.2.clearance"),
          TariffContentKey("tariff.declaration.item.commodityMeasure.3.clearance"),
          TariffContentKey("tariff.declaration.item.commodityMeasure.4.clearance")
        )
      case _ =>
        Seq(
          TariffContentKey("tariff.declaration.item.commodityMeasure.1.common"),
          TariffContentKey("tariff.declaration.item.commodityMeasure.2.common"),
          TariffContentKey("tariff.declaration.item.commodityMeasure.3.common")
        )
    }
}
