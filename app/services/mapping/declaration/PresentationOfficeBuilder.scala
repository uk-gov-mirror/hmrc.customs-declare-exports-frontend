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

package services.mapping.declaration

import forms.Choice
import forms.Choice.AllowedChoiceValues
import forms.declaration.officeOfExit.{OfficeOfExit, OfficeOfExitStandard}
import uk.gov.hmrc.http.cache.client.CacheMap
import wco.datamodel.wco.dec_dms._2.Declaration
import wco.datamodel.wco.dec_dms._2.Declaration.PresentationOffice
import wco.datamodel.wco.declaration_ds.dms._2._

object PresentationOfficeBuilder {

  def build(implicit cacheMap: CacheMap): Declaration.PresentationOffice =
    cacheMap
      .getEntry[Choice](Choice.choiceId)
      .filter(choice => choice.value.equals(AllowedChoiceValues.StandardDec))
      .map(buildPresentationOffice)
      .orNull

  private def buildPresentationOffice(choice: Choice)(implicit cacheMap: CacheMap): Declaration.PresentationOffice =
    cacheMap
      .getEntry[OfficeOfExitStandard](OfficeOfExit.formId)
      .map(createPresentationOffice)
      .orNull

  private def createPresentationOffice(data: OfficeOfExitStandard): Declaration.PresentationOffice = {
    val presentationOfficeId = new PresentationOfficeIdentificationIDType()
    presentationOfficeId.setValue(data.presentationOfficeId)

    val presentationOffice = new PresentationOffice()
    presentationOffice.setID(presentationOfficeId)
    presentationOffice
  }
}