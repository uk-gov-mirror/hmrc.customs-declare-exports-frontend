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
import forms.supplementary.{AdditionalDeclarationType, DispatchLocation}
import uk.gov.hmrc.http.cache.client.CacheMap

case class DeclarationType(
  dispatchLocation: Option[DispatchLocation],
  additionalDeclarationType: Option[AdditionalDeclarationType]
) extends SummaryContainer with MetadataPropertiesConvertable {

  override def toMetadataProperties(): Map[String, String] = {
    val propertiesKey = "declaration.typeCode"
    val propertiesValue = dispatchLocation.map(_.dispatchLocation).getOrElse("") +
      additionalDeclarationType.map(_.additionalDeclarationType).getOrElse("")
    Map(propertiesKey -> propertiesValue)
  }

  override def isEmpty: Boolean = dispatchLocation.isEmpty && additionalDeclarationType.isEmpty
}

object DeclarationType {
  val id = "DeclarationType"

  def apply(cacheMap: CacheMap): DeclarationType = DeclarationType(
    dispatchLocation = cacheMap.getEntry[DispatchLocation](DispatchLocation.formId),
    additionalDeclarationType = cacheMap.getEntry[AdditionalDeclarationType](AdditionalDeclarationType.formId)
  )
}