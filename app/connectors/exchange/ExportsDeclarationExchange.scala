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

package connectors.exchange

import java.time.Instant

import forms.declaration._
import forms.declaration.additionaldeclarationtype.AdditionalDeclarationType.AdditionalDeclarationType
import models.DeclarationStatus.DeclarationStatus
import models.DeclarationType.DeclarationType
import models.ExportsDeclaration
import models.declaration._
import play.api.libs.json.{Format, Json}

case class TotalItemsExchange(totalAmountInvoiced: Option[String], exchangeRate: Option[String], totalPackage: Option[String])

object TotalItemsExchange {
  implicit val format: Format[TotalItemsExchange] = Json.format[TotalItemsExchange]
}

case class ExportsDeclarationExchange(
  id: Option[String] = None,
  status: DeclarationStatus,
  createdDateTime: Instant,
  updatedDateTime: Instant,
  sourceId: Option[String],
  `type`: DeclarationType,
  dispatchLocation: Option[DispatchLocation] = None,
  additionalDeclarationType: Option[AdditionalDeclarationType] = None,
  consignmentReferences: Option[ConsignmentReferences] = None,
  transport: Transport = Transport(),
  parties: Parties = Parties(),
  locations: Locations = Locations(),
  items: Seq[ExportItem] = Seq.empty[ExportItem],
  totalNumberOfItems: Option[TotalItemsExchange] = None,
  previousDocuments: Option[PreviousDocumentsData] = None,
  natureOfTransaction: Option[NatureOfTransaction] = None
) {
  def toExportsDeclaration: ExportsDeclaration = ExportsDeclaration(
    id = this.id.get,
    status = this.status,
    createdDateTime = this.createdDateTime,
    updatedDateTime = this.updatedDateTime,
    sourceId = this.sourceId,
    `type` = this.`type`,
    dispatchLocation = this.dispatchLocation,
    additionalDeclarationType = this.additionalDeclarationType,
    consignmentReferences = this.consignmentReferences,
    transport = this.transport,
    parties = this.parties,
    locations = this.locations,
    items = this.items,
    totalNumberOfItems = this.totalNumberOfItems.flatMap { exchange =>
      (exchange.totalAmountInvoiced, exchange.exchangeRate) match {
        case (None, None)                        => None
        case (totalAmountInvoiced, exchangeRate) => Some(TotalNumberOfItems(exchangeRate, totalAmountInvoiced))
      }
    },
    totalPackageQuantity = this.totalNumberOfItems.map(exchange => TotalPackageQuantity(exchange.totalPackage)),
    previousDocuments = this.previousDocuments,
    natureOfTransaction = this.natureOfTransaction
  )
}

object ExportsDeclarationExchange {

  import play.api.libs.json._

  implicit val format: OFormat[ExportsDeclarationExchange] = Json.format[ExportsDeclarationExchange]

  private def buildDeclaration(declaration: ExportsDeclaration, idProvider: ExportsDeclaration => Option[String]): ExportsDeclarationExchange =
    ExportsDeclarationExchange(
      id = idProvider(declaration),
      status = declaration.status,
      createdDateTime = declaration.createdDateTime,
      updatedDateTime = declaration.updatedDateTime,
      sourceId = declaration.sourceId,
      `type` = declaration.`type`,
      dispatchLocation = declaration.dispatchLocation,
      additionalDeclarationType = declaration.additionalDeclarationType,
      consignmentReferences = declaration.consignmentReferences,
      transport = declaration.transport,
      parties = declaration.parties,
      locations = declaration.locations,
      items = declaration.items,
      totalNumberOfItems = if (declaration.totalNumberOfItems.isDefined || declaration.totalPackageQuantity.isDefined) {
        Some(
          TotalItemsExchange(
            declaration.totalNumberOfItems.flatMap(_.totalAmountInvoiced),
            declaration.totalNumberOfItems.flatMap(_.exchangeRate),
            declaration.totalPackageQuantity.flatMap(_.totalPackage)
          )
        )
      } else None,
      previousDocuments = declaration.previousDocuments,
      natureOfTransaction = declaration.natureOfTransaction
    )

  def apply(declaration: ExportsDeclaration): ExportsDeclarationExchange =
    buildDeclaration(declaration, declaration => Some(declaration.id))

  def withoutId(declaration: ExportsDeclaration): ExportsDeclarationExchange =
    buildDeclaration(declaration, _ => None)
}
