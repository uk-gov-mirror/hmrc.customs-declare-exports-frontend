/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.navigation

import config.AppConfig
import controllers.util.{Add, FormAction, Remove, SaveAndReturn}
import forms.Choice.AllowedChoiceValues
import forms.declaration.RoutingQuestionYesNo.{ChangeCountryPage, RemoveCountryPage, RoutingQuestionPage}
import forms.declaration.additionaldeclarationtype.AdditionalDeclarationTypeStandardDec
import forms.declaration.additionaldocuments.DocumentsProduced
import forms.declaration.countries.Countries.{DestinationCountryPage, OriginationCountryPage}
import forms.declaration.officeOfExit.{OfficeOfExitInsideUK, OfficeOfExitOutsideUK}
import forms.declaration.{BorderTransport, Document, PackageInformation, _}
import forms.{Choice, DeclarationPage}
import javax.inject.Inject
import models.DeclarationType._
import models.Mode
import models.Mode.ErrorFix
import models.declaration.ExportItem
import models.requests.{ExportsSessionKeys, JourneyRequest}
import models.responses.FlashKeys
import play.api.mvc.{AnyContent, Call, Result, Results}
import services.audit.{AuditService, AuditTypes}
import uk.gov.hmrc.http.HeaderCarrier

class Navigator @Inject()(appConfig: AppConfig, auditService: AuditService) {
  
  def continueTo(mode: Mode, factory: Mode => Call)(implicit req: JourneyRequest[AnyContent], hc: HeaderCarrier): Result =
    (mode, FormAction.bindFromRequest) match {
      case (ErrorFix, Add) | (ErrorFix, Remove(_)) => Results.Redirect(factory(mode))
      case (ErrorFix, _) if (req.sourceDecId.isDefined) =>
        Results.Redirect(controllers.routes.RejectedNotificationsController.displayPage(req.sourceDecId.get))
      case (ErrorFix, _) => Results.Redirect(controllers.routes.SubmissionsController.displayListOfSubmissions())
      case (_, SaveAndReturn) =>
        auditService.auditAllPagesUserInput(AuditTypes.SaveAndReturnSubmission, req.cacheModel)
        goToDraftConfirmation()
      case _ => Results.Redirect(factory(mode))
    }

  private def goToDraftConfirmation()(implicit req: JourneyRequest[_]): Result = {
    val updatedDateTime = req.cacheModel.updatedDateTime
    val expiry = updatedDateTime.plusSeconds(appConfig.draftTimeToLive.toSeconds)
    Results
      .Redirect(controllers.declaration.routes.ConfirmationController.displayDraftConfirmation())
      .flashing(FlashKeys.expiryDate -> expiry.toEpochMilli.toString)
      .removingFromSession(ExportsSessionKeys.declarationId)
  }

}

case class ItemId(id: String)

object Navigator {

  val standard: PartialFunction[DeclarationPage, Mode => Call] = {
    case ConsigneeDetails            => controllers.declaration.routes.CarrierDetailsController.displayPage
    case BorderTransport             => controllers.declaration.routes.DepartureTransportController.displayPage
    case TransportPayment            => controllers.declaration.routes.BorderTransportController.displayPage
    case ContainerFirst              => controllers.declaration.routes.TransportPaymentController.displayPage
    case ContainerAdd                => controllers.declaration.routes.TransportContainerController.displayContainerSummary
    case Document                    => controllers.declaration.routes.NatureOfTransactionController.displayPage
    case OriginationCountryPage      => controllers.declaration.routes.DeclarationHolderController.displayPage
    case DestinationCountryPage      => controllers.declaration.routes.OriginationCountryController.displayPage
    case RoutingQuestionPage         => controllers.declaration.routes.DestinationCountryController.displayPage
    case RemoveCountryPage           => controllers.declaration.routes.RoutingCountriesSummaryController.displayPage
    case ChangeCountryPage           => controllers.declaration.routes.RoutingCountriesSummaryController.displayPage
    case GoodsLocationForm           => controllers.declaration.routes.RoutingCountriesSummaryController.displayPage
    case DeclarationHolder           => controllers.declaration.routes.DeclarationAdditionalActorsController.displayPage
    case SupervisingCustomsOffice    => controllers.declaration.routes.WarehouseIdentificationController.displayPage
    case InlandModeOfTransportCode   => controllers.declaration.routes.SupervisingCustomsOfficeController.displayPage
    case TransportLeavingTheBorder   => controllers.declaration.routes.InlandTransportDetailsController.displayPage
    case WarehouseIdentification     => controllers.declaration.routes.ItemsSummaryController.displayPage
    case DeclarationAdditionalActors => controllers.declaration.routes.ConsigneeDetailsController.displayPage
    case TotalPackageQuantity        => controllers.declaration.routes.TotalNumberOfItemsController.displayPage
    case page                        => throw new IllegalArgumentException(s"Navigator back-link route not implemented for $page on standard")
  }
  val standardItemPage: PartialFunction[DeclarationPage, (Mode, String) => Call] = {
    case PackageInformation    => controllers.declaration.routes.StatisticalValueController.displayPage
    case AdditionalInformation => controllers.declaration.routes.CommodityMeasureController.displayPage
    case CusCode               => controllers.declaration.routes.UNDangerousGoodsCodeController.displayPage
    case NactCode              => controllers.declaration.routes.TaricCodeController.displayPage
    case page                  => throw new IllegalArgumentException(s"Navigator back-link route not implemented for $page on standard")
  }

  val clearance: PartialFunction[DeclarationPage, Mode => Call] = {
    case ConsigneeDetails          => controllers.declaration.routes.CarrierDetailsController.displayPage
    case TransportPayment          => controllers.declaration.routes.DepartureTransportController.displayPage
    case ContainerFirst            => controllers.declaration.routes.TransportPaymentController.displayPage
    case ContainerAdd              => controllers.declaration.routes.TransportContainerController.displayContainerSummary
    case Document                  => controllers.declaration.routes.OfficeOfExitController.displayPage
    case DestinationCountryPage    => controllers.declaration.routes.DeclarationHolderController.displayPage
    case RoutingQuestionPage       => controllers.declaration.routes.DestinationCountryController.displayPage
    case RemoveCountryPage         => controllers.declaration.routes.RoutingCountriesSummaryController.displayPage
    case ChangeCountryPage         => controllers.declaration.routes.RoutingCountriesSummaryController.displayPage
    case GoodsLocationForm         => controllers.declaration.routes.DestinationCountryController.displayPage
    case DeclarationHolder         => controllers.declaration.routes.ConsigneeDetailsController.displayPage
    case OfficeOfExitInsideUK      => controllers.declaration.routes.LocationController.displayPage
    case OfficeOfExitOutsideUK     => controllers.declaration.routes.OfficeOfExitController.displayPage
    case SupervisingCustomsOffice  => controllers.declaration.routes.WarehouseIdentificationController.displayPage
    case TransportLeavingTheBorder => controllers.declaration.routes.SupervisingCustomsOfficeController.displayPage
    case WarehouseIdentification   => controllers.declaration.routes.ItemsSummaryController.displayPage
    case TotalPackageQuantity      => controllers.declaration.routes.OfficeOfExitController.displayPage
    case page                      => throw new IllegalArgumentException(s"Navigator back-link route not implemented for $page on clearance")
  }

  val clearanceItemPage: PartialFunction[DeclarationPage, (Mode, String) => Call] = {
    case PackageInformation    => controllers.declaration.routes.CommodityDetailsController.displayPage
    case AdditionalInformation => controllers.declaration.routes.CommodityMeasureController.displayPage
    case page                  => throw new IllegalArgumentException(s"Navigator back-link route not implemented for $page on clearance")
  }

  val supplementary: PartialFunction[DeclarationPage, Mode => Call] = {
    case ConsigneeDetails            => controllers.declaration.routes.RepresentativeStatusController.displayPage
    case BorderTransport             => controllers.declaration.routes.DepartureTransportController.displayPage
    case ContainerFirst              => controllers.declaration.routes.BorderTransportController.displayPage
    case ContainerAdd                => controllers.declaration.routes.TransportContainerController.displayContainerSummary
    case Document                    => controllers.declaration.routes.NatureOfTransactionController.displayPage
    case OriginationCountryPage      => controllers.declaration.routes.DeclarationHolderController.displayPage
    case DestinationCountryPage      => controllers.declaration.routes.OriginationCountryController.displayPage
    case GoodsLocationForm           => controllers.declaration.routes.DestinationCountryController.displayPage
    case OfficeOfExitInsideUK        => controllers.declaration.routes.LocationController.displayPage
    case OfficeOfExitOutsideUK       => controllers.declaration.routes.OfficeOfExitController.displayPage
    case DeclarationHolder           => controllers.declaration.routes.DeclarationAdditionalActorsController.displayPage
    case SupervisingCustomsOffice    => controllers.declaration.routes.WarehouseIdentificationController.displayPage
    case InlandModeOfTransportCode   => controllers.declaration.routes.SupervisingCustomsOfficeController.displayPage
    case TransportLeavingTheBorder   => controllers.declaration.routes.InlandTransportDetailsController.displayPage
    case WarehouseIdentification     => controllers.declaration.routes.ItemsSummaryController.displayPage
    case DeclarationAdditionalActors => controllers.declaration.routes.ConsigneeDetailsController.displayPage
    case TotalPackageQuantity        => controllers.declaration.routes.TotalNumberOfItemsController.displayPage
    case page                        => throw new IllegalArgumentException(s"Navigator back-link route not implemented for $page on supplementary")
  }
  val supplementaryItemPage: PartialFunction[DeclarationPage, (Mode, String) => Call] = {
    case PackageInformation    => controllers.declaration.routes.StatisticalValueController.displayPage
    case AdditionalInformation => controllers.declaration.routes.CommodityMeasureController.displayPage
    case CusCode               => controllers.declaration.routes.UNDangerousGoodsCodeController.displayPage
    case NactCode              => controllers.declaration.routes.TaricCodeController.displayPage
    case page                  => throw new IllegalArgumentException(s"Navigator back-link route not implemented for $page on supplementary")
  }

  val simplified: PartialFunction[DeclarationPage, Mode => Call] = {
    case ConsigneeDetails            => controllers.declaration.routes.CarrierDetailsController.displayPage
    case DeclarationAdditionalActors => controllers.declaration.routes.ConsigneeDetailsController.displayPage
    case TransportPayment            => controllers.declaration.routes.SupervisingCustomsOfficeController.displayPage
    case ContainerFirst              => controllers.declaration.routes.TransportPaymentController.displayPage
    case ContainerAdd                => controllers.declaration.routes.TransportContainerController.displayContainerSummary
    case Document                    => controllers.declaration.routes.OfficeOfExitController.displayPage
    case DestinationCountryPage      => controllers.declaration.routes.DeclarationHolderController.displayPage
    case RoutingQuestionPage         => controllers.declaration.routes.DestinationCountryController.displayPage
    case RemoveCountryPage           => controllers.declaration.routes.RoutingCountriesSummaryController.displayPage
    case ChangeCountryPage           => controllers.declaration.routes.RoutingCountriesSummaryController.displayPage
    case GoodsLocationForm           => controllers.declaration.routes.RoutingCountriesSummaryController.displayPage
    case DeclarationHolder           => controllers.declaration.routes.DeclarationAdditionalActorsController.displayPage
    case SupervisingCustomsOffice    => controllers.declaration.routes.WarehouseIdentificationController.displayPage
    case InlandModeOfTransportCode   => controllers.declaration.routes.SupervisingCustomsOfficeController.displayPage
    case TransportLeavingTheBorder   => controllers.declaration.routes.InlandTransportDetailsController.displayPage
    case WarehouseIdentification     => controllers.declaration.routes.ItemsSummaryController.displayPage
    case TotalPackageQuantity        => controllers.declaration.routes.TotalNumberOfItemsController.displayPage
    case page                        => throw new IllegalArgumentException(s"Navigator back-link route not implemented for $page on simplified")
  }
  val simplifiedItemPage: PartialFunction[DeclarationPage, (Mode, String) => Call] = {
    case PackageInformation    => controllers.declaration.routes.NactCodeController.displayPage
    case AdditionalInformation => controllers.declaration.routes.PackageInformationController.displayPage
    case CusCode               => controllers.declaration.routes.UNDangerousGoodsCodeController.displayPage
    case NactCode              => controllers.declaration.routes.TaricCodeController.displayPage
    case page                  => throw new IllegalArgumentException(s"Navigator back-link route not implemented for $page on simplified")
  }

  val occasional: PartialFunction[DeclarationPage, Mode => Call] = {
    case ConsigneeDetails            => controllers.declaration.routes.CarrierDetailsController.displayPage
    case DeclarationAdditionalActors => controllers.declaration.routes.ConsigneeDetailsController.displayPage
    case TransportPayment            => controllers.declaration.routes.SupervisingCustomsOfficeController.displayPage
    case ContainerFirst              => controllers.declaration.routes.TransportPaymentController.displayPage
    case ContainerAdd                => controllers.declaration.routes.TransportContainerController.displayContainerSummary
    case Document                    => controllers.declaration.routes.OfficeOfExitController.displayPage
    case DestinationCountryPage      => controllers.declaration.routes.DeclarationHolderController.displayPage
    case RoutingQuestionPage         => controllers.declaration.routes.DestinationCountryController.displayPage
    case RemoveCountryPage           => controllers.declaration.routes.RoutingCountriesSummaryController.displayPage
    case GoodsLocationForm           => controllers.declaration.routes.RoutingCountriesSummaryController.displayPage
    case DeclarationHolder           => controllers.declaration.routes.DeclarationAdditionalActorsController.displayPage
    case ChangeCountryPage           => controllers.declaration.routes.RoutingCountriesSummaryController.displayPage
    case SupervisingCustomsOffice    => controllers.declaration.routes.WarehouseIdentificationController.displayPage
    case InlandModeOfTransportCode   => controllers.declaration.routes.SupervisingCustomsOfficeController.displayPage
    case TransportLeavingTheBorder   => controllers.declaration.routes.InlandTransportDetailsController.displayPage
    case WarehouseIdentification     => controllers.declaration.routes.ItemsSummaryController.displayPage
    case TotalPackageQuantity        => controllers.declaration.routes.TotalNumberOfItemsController.displayPage
    case page                        => throw new IllegalArgumentException(s"Navigator back-link route not implemented for $page on occasional")
  }

  val occasionalItemPage: PartialFunction[DeclarationPage, (Mode, String) => Call] = {
    case PackageInformation    => controllers.declaration.routes.NactCodeController.displayPage
    case AdditionalInformation => controllers.declaration.routes.PackageInformationController.displayPage
    case CusCode               => controllers.declaration.routes.UNDangerousGoodsCodeController.displayPage
    case NactCode              => controllers.declaration.routes.TaricCodeController.displayPage
    case page                  => throw new IllegalArgumentException(s"Navigator back-link route not implemented for $page on occasional")
  }

  val common: PartialFunction[DeclarationPage, Mode => Call] = {
    case DeclarationChoice =>
      _ =>
        controllers.routes.ChoiceController.displayPage(Some(Choice(AllowedChoiceValues.CreateDec)))
    case DispatchLocation                     => controllers.declaration.routes.DeclarationChoiceController.displayPage
    case ConsignmentReferences                => controllers.declaration.routes.AdditionalDeclarationTypeController.displayPage
    case DeclarantDetails                     => controllers.declaration.routes.ConsignmentReferencesController.displayPage
    case DeclarantIsExporter                  => controllers.declaration.routes.DeclarantDetailsController.displayPage
    case ExporterDetails                      => controllers.declaration.routes.DeclarantExporterController.displayPage
    case RepresentativeAgent                  => controllers.declaration.routes.ExporterDetailsController.displayPage
    case RepresentativeEntity                 => controllers.declaration.routes.RepresentativeAgentController.displayPage
    case RepresentativeStatus                 => controllers.declaration.routes.RepresentativeEntityController.displayPage
    case CarrierDetails                       => controllers.declaration.routes.RepresentativeStatusController.displayPage
    case OfficeOfExitInsideUK                 => controllers.declaration.routes.LocationController.displayPage
    case OfficeOfExitOutsideUK                => controllers.declaration.routes.OfficeOfExitController.displayPage
    case AdditionalDeclarationTypeStandardDec => controllers.declaration.routes.DispatchLocationController.displayPage
    case TotalNumberOfItems                   => controllers.declaration.routes.OfficeOfExitController.displayPage
    case NatureOfTransaction                  => controllers.declaration.routes.TotalPackageQuantityController.displayPage
    case ProcedureCodes                       => controllers.declaration.routes.ItemsSummaryController.displayPage
    case DepartureTransport                   => controllers.declaration.routes.TransportLeavingTheBorderController.displayPage
    case ExportItem                           => controllers.declaration.routes.PreviousDocumentsController.displayPage
  }

  val commonItem: PartialFunction[DeclarationPage, (Mode, String) => Call] = {
    case FiscalInformation         => controllers.declaration.routes.ProcedureCodesController.displayPage
    case AdditionalFiscalReference => controllers.declaration.routes.FiscalInformationController.displayPage(_, _, fastForward = false)
    case CommodityDetails          => controllers.declaration.routes.FiscalInformationController.displayPage(_, _, fastForward = true)
    case UNDangerousGoodsCode      => controllers.declaration.routes.CommodityDetailsController.displayPage
    case TaricCode                 => controllers.declaration.routes.CusCodeController.displayPage
    case StatisticalValue          => controllers.declaration.routes.NactCodeController.displayPage
    case CommodityMeasure          => controllers.declaration.routes.PackageInformationController.displayPage
    case DocumentsProduced         => controllers.declaration.routes.AdditionalInformationController.displayPage
  }

  def backLink(page: DeclarationPage, mode: Mode)(implicit request: JourneyRequest[_]): Call =
    mode match {
      case Mode.ErrorFix if (request.sourceDecId.isDefined) => controllers.routes.RejectedNotificationsController.displayPage(request.sourceDecId.get)
      case Mode.ErrorFix                                    => controllers.routes.SubmissionsController.displayListOfSubmissions()
      case Mode.Change                                      => controllers.declaration.routes.SummaryController.displayPage(Mode.Normal)
      case Mode.ChangeAmend                                 => controllers.declaration.routes.SummaryController.displayPage(Mode.Amend)
      case Mode.Draft                                       => controllers.declaration.routes.SummaryController.displayPage(Mode.Draft)
      case _ =>
        val specific = request.declarationType match {
          case STANDARD      => standard
          case SUPPLEMENTARY => supplementary
          case SIMPLIFIED    => simplified
          case OCCASIONAL    => occasional
          case CLEARANCE     => clearance
        }
        common.orElse(specific)(page)(mode)
    }

  def backLink(page: DeclarationPage, mode: Mode, itemId: ItemId)(implicit request: JourneyRequest[_]): Call =
    mode match {
      case Mode.ErrorFix if (request.sourceDecId.isDefined) => controllers.routes.RejectedNotificationsController.displayPage(request.sourceDecId.get)
      case Mode.ErrorFix                                    => controllers.routes.SubmissionsController.displayListOfSubmissions()
      case Mode.Change                                      => controllers.declaration.routes.SummaryController.displayPage(Mode.Normal)
      case Mode.ChangeAmend                                 => controllers.declaration.routes.SummaryController.displayPage(Mode.Amend)
      case Mode.Draft                                       => controllers.declaration.routes.SummaryController.displayPage(Mode.Draft)
      case _ =>
        val specific = request.declarationType match {
          case STANDARD      => standardItemPage
          case SUPPLEMENTARY => supplementaryItemPage
          case SIMPLIFIED    => simplifiedItemPage
          case OCCASIONAL    => occasionalItemPage
          case CLEARANCE     => clearanceItemPage
        }
        commonItem.orElse(specific)(page)(mode, itemId.id)
    }
}
