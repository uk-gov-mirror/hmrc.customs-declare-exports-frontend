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

package services.cache.mapping.declaration

import services.cache.ExportsCacheModel
import services.mapping.AuthorisationHoldersBuilder
import services.mapping.declaration._
import services.mapping.declaration.consignment.DeclarationConsignmentBuilder
import services.mapping.goodsshipment.GoodsShipmentBuilder
import wco.datamodel.wco.dec_dms._2.Declaration

object DeclarationBuilder {

  val defaultFunctionCode = "9"

  def build(exportsCacheModel: ExportsCacheModel): Declaration = {
    val declaration = new Declaration()

    declaration.setFunctionCode(FunctionCodeBuilder.build(defaultFunctionCode))

    FunctionalReferenceIdBuilder.buildThenAdd(exportsCacheModel, declaration)
    TypeCodeBuilder.buildThenAdd(exportsCacheModel, declaration)

    GoodsItemQuantityBuilder.buildThenAdd(exportsCacheModel, declaration)

    AgentBuilder.buildThenAdd(exportsCacheModel, declaration)

    declaration.setGoodsShipment(GoodsShipmentBuilder.build(exportsCacheModel))
    //    declaration.setExitOffice(ExitOfficeBuilder.build)
    //    declaration.setBorderTransportMeans(BorderTransportMeansBuilder.build)
    //    declaration.setExporter(ExporterBuilder.build)
    //    declaration.setDeclarant(DeclarantBuilder.build)
    //    declaration.setInvoiceAmount(InvoiceAmountBuilder.build)
    //    declaration.setPresentationOffice(PresentationOfficeBuilder.build)
    //    declaration.setSpecificCircumstancesCodeCode(SpecificCircumstancesCodeBuilder.build)
    //    declaration.setSupervisingOffice(SupervisingOfficeBuilder.build)
    //    declaration.setTotalPackageQuantity(TotalPackageQuantityBuilder.build)
    DeclarationConsignmentBuilder.buildThenAdd(exportsCacheModel, declaration)
    AuthorisationHoldersBuilder.buildThenAdd(exportsCacheModel, declaration)
    CurrencyExchangeBuilder.buildThenAdd(exportsCacheModel, declaration)
    declaration
  }

}