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

package models.declaration.supplementary

import forms.MetadataPropertiesConvertable
import forms.supplementary.AdditionalInformation
import play.api.libs.json.Json

case class AdditionalInformationData(items: Seq[AdditionalInformation]) extends MetadataPropertiesConvertable {
  override def toMetadataProperties(): Map[String, String] =
    items.zipWithIndex.map { itemWithId =>
      Map(
        "declaration.goodsShipment.governmentAgencyGoodsItems[0].additionalInformations[" + itemWithId._2 + "].statementCode" -> itemWithId._1.code
          .getOrElse(""),
        "declaration.goodsShipment.governmentAgencyGoodsItems[0].additionalInformations[" + itemWithId._2 + "].statementDescription" -> itemWithId._1.description
          .getOrElse("")
      )
    }.fold(Map.empty)(_ ++ _)

  def containsItem(item: AdditionalInformation): Boolean = items.contains(item)
}

object AdditionalInformationData {
  implicit val format = Json.format[AdditionalInformationData]

  val formId = "AdditionalInformationData"

  val maxNumberOfItems = 99
}
