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

package forms.supplementary

import forms.supplementary.ItemType._
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue}

class ItemTypeSpec extends WordSpec with MustMatchers {
  import ItemTypeSpec._

  "ItemType" should {

    "convert itself into Item Type properties" when {
      "provided with mandatory data only" in {
        val itemType = ItemType(
          combinedNomenclatureCode = combinedNomenclatureCode,
          taricAdditionalCodes = Nil,
          nationalAdditionalCodes = Nil,
          descriptionOfGoods = descriptionOfGoods,
          cusCode = None,
          statisticalValue = statisticalValue
        )
        val expectedItemTypeProperties: Map[String, String] = Map(
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[0].id" -> combinedNomenclatureCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[0].identificationTypeCode" ->
            IdentificationTypeCodes.CombinedNomenclatureCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.description" -> descriptionOfGoods,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].statisticalValueAmount" -> statisticalValue
        )

        itemType.toMetadataProperties() must equal(expectedItemTypeProperties)
      }

      "provided with mandatory data and single TARIC Additional Code" in {
        val itemType = ItemType(
          combinedNomenclatureCode = combinedNomenclatureCode,
          taricAdditionalCodes = Seq(taricAdditionalCode),
          nationalAdditionalCodes = Nil,
          descriptionOfGoods = descriptionOfGoods,
          cusCode = None,
          statisticalValue = statisticalValue
        )
        val expectedItemTypeProperties: Map[String, String] = Map(
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[0].id" -> combinedNomenclatureCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[0].identificationTypeCode" ->
            IdentificationTypeCodes.CombinedNomenclatureCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[1].id" -> taricAdditionalCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[1].identificationTypeCode" ->
            IdentificationTypeCodes.TARICAdditionalCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.description" -> descriptionOfGoods,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].statisticalValueAmount" -> statisticalValue
        )

        itemType.toMetadataProperties() must equal(expectedItemTypeProperties)
      }

      "provided with mandatory data and multiple TARIC Additional Codes" in {
        val taricAdditionalCode_1 = "AB12"
        val taricAdditionalCode_2 = "CD34"
        val taricAdditionalCode_3 = "56EF"
        val itemType = ItemType(
          combinedNomenclatureCode = combinedNomenclatureCode,
          taricAdditionalCodes = Seq(taricAdditionalCode_1, taricAdditionalCode_2, taricAdditionalCode_3),
          nationalAdditionalCodes = Nil,
          descriptionOfGoods = descriptionOfGoods,
          cusCode = None,
          statisticalValue = statisticalValue
        )
        val expectedItemTypeProperties: Map[String, String] = Map(
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[0].id" -> combinedNomenclatureCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[0].identificationTypeCode" ->
            IdentificationTypeCodes.CombinedNomenclatureCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[1].id" -> taricAdditionalCode_1,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[1].identificationTypeCode" ->
            IdentificationTypeCodes.TARICAdditionalCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[2].id" -> taricAdditionalCode_2,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[2].identificationTypeCode" ->
            IdentificationTypeCodes.TARICAdditionalCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[3].id" -> taricAdditionalCode_3,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[3].identificationTypeCode" ->
            IdentificationTypeCodes.TARICAdditionalCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.description" -> descriptionOfGoods,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].statisticalValueAmount" -> statisticalValue
        )

        itemType.toMetadataProperties() must equal(expectedItemTypeProperties)
      }

      "provided with mandatory data and single National Additional Code" in {
        val itemType = ItemType(
          combinedNomenclatureCode = combinedNomenclatureCode,
          taricAdditionalCodes = Nil,
          nationalAdditionalCodes = Seq(nationalAdditionalCode),
          descriptionOfGoods = descriptionOfGoods,
          cusCode = None,
          statisticalValue = statisticalValue
        )
        val expectedItemTypeProperties: Map[String, String] = Map(
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[0].id" -> combinedNomenclatureCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[0].identificationTypeCode" ->
            IdentificationTypeCodes.CombinedNomenclatureCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[1].id" -> nationalAdditionalCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[1].identificationTypeCode" ->
            IdentificationTypeCodes.NationalAdditionalCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.description" -> descriptionOfGoods,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].statisticalValueAmount" -> statisticalValue
        )

        itemType.toMetadataProperties() must equal(expectedItemTypeProperties)
      }

      "provided with mandatory data and multiple National Additional Codes" in {
        val nationalAdditionalCode_1 = "AB12"
        val nationalAdditionalCode_2 = "CD34"
        val nationalAdditionalCode_3 = "56EF"
        val itemType = ItemType(
          combinedNomenclatureCode = combinedNomenclatureCode,
          taricAdditionalCodes = Nil,
          nationalAdditionalCodes = Seq(nationalAdditionalCode_1, nationalAdditionalCode_2, nationalAdditionalCode_3),
          descriptionOfGoods = descriptionOfGoods,
          cusCode = None,
          statisticalValue = statisticalValue
        )
        val expectedItemTypeProperties: Map[String, String] = Map(
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[0].id" -> combinedNomenclatureCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[0].identificationTypeCode" ->
            IdentificationTypeCodes.CombinedNomenclatureCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[1].id" -> nationalAdditionalCode_1,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[1].identificationTypeCode" ->
            IdentificationTypeCodes.NationalAdditionalCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[2].id" -> nationalAdditionalCode_2,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[2].identificationTypeCode" ->
            IdentificationTypeCodes.NationalAdditionalCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[3].id" -> nationalAdditionalCode_3,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[3].identificationTypeCode" ->
            IdentificationTypeCodes.NationalAdditionalCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.description" -> descriptionOfGoods,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].statisticalValueAmount" -> statisticalValue
        )

        itemType.toMetadataProperties() must equal(expectedItemTypeProperties)
      }

      "provided with mandatory data and CUS Code" in {
        val itemType = ItemType(
          combinedNomenclatureCode = combinedNomenclatureCode,
          taricAdditionalCodes = Nil,
          nationalAdditionalCodes = Nil,
          descriptionOfGoods = descriptionOfGoods,
          cusCode = Some(cusCode),
          statisticalValue = statisticalValue
        )
        val expectedItemTypeProperties: Map[String, String] = Map(
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[0].id" -> combinedNomenclatureCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[0].identificationTypeCode" ->
            IdentificationTypeCodes.CombinedNomenclatureCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[1].id" -> cusCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[1].identificationTypeCode" ->
            IdentificationTypeCodes.CUSCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.description" -> descriptionOfGoods,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].statisticalValueAmount" -> statisticalValue
        )

        itemType.toMetadataProperties() must equal(expectedItemTypeProperties)
      }

      "provided with all data" in {
        val itemType = ItemType(
          combinedNomenclatureCode = combinedNomenclatureCode,
          taricAdditionalCodes = Seq(taricAdditionalCode),
          nationalAdditionalCodes = Seq(nationalAdditionalCode),
          descriptionOfGoods = descriptionOfGoods,
          cusCode = Some(cusCode),
          statisticalValue = statisticalValue
        )
        val expectedItemTypeProperties: Map[String, String] = Map(
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[0].id" -> combinedNomenclatureCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[0].identificationTypeCode" ->
            IdentificationTypeCodes.CombinedNomenclatureCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[1].id" -> taricAdditionalCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[1].identificationTypeCode" ->
            IdentificationTypeCodes.TARICAdditionalCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[2].id" -> nationalAdditionalCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[2].identificationTypeCode" ->
            IdentificationTypeCodes.NationalAdditionalCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[3].id" -> cusCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.classifications[3].identificationTypeCode" ->
            IdentificationTypeCodes.CUSCode,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].commodity.description" -> descriptionOfGoods,
          "declaration.goodsShipment.governmentAgencyGoodsItems[0].statisticalValueAmount" -> statisticalValue
        )

        itemType.toMetadataProperties() must equal(expectedItemTypeProperties)
      }
    }
  }

}

object ItemTypeSpec {
  private val combinedNomenclatureCode = "ABCD1234"
  private val taricAdditionalCode = "AB12"
  private val nationalAdditionalCode = "CD34"
  private val descriptionOfGoods = "Description of goods."
  private val cusCode = "QWER0987"
  private val statisticalValue = "1234567890123.45"

  val correctItemType = ItemType(
    combinedNomenclatureCode = combinedNomenclatureCode,
    taricAdditionalCodes = Seq(taricAdditionalCode),
    nationalAdditionalCodes = Seq(nationalAdditionalCode),
    descriptionOfGoods = descriptionOfGoods,
    cusCode = Some(cusCode),
    statisticalValue = statisticalValue
  )
  val emptyItemType = ItemType(
    combinedNomenclatureCode = "",
    taricAdditionalCodes = Nil,
    nationalAdditionalCodes = Nil,
    descriptionOfGoods = "",
    cusCode = None,
    statisticalValue = ""
  )

  val correctItemTypeJSON: JsValue = JsObject(
    Map(
      "combinedNomenclatureCode" -> JsString(combinedNomenclatureCode),
      "taricAdditionalCode" -> JsArray(Seq(JsString(taricAdditionalCode))),
      "nationalAdditionalCode" -> JsArray(Seq(JsString(nationalAdditionalCode))),
      "descriptionOfGoods" -> JsString(descriptionOfGoods),
      "cusCode" -> JsString(cusCode),
      "statisticalValue" -> JsString(statisticalValue)
    )
  )
  val emptyItemTypeJSON: JsValue = JsObject(
    Map(
      "combinedNomenclatureCode" -> JsString(""),
      "taricAdditionalCode" -> JsString(""),
      "nationalAdditionalCode" -> JsString(""),
      "descriptionOfGoods" -> JsString(""),
      "cusCode" -> JsString(""),
      "statisticalValue" -> JsString("")
    )
  )

}
