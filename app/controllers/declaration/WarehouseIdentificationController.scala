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

package controllers.declaration

import controllers.actions.{AuthAction, JourneyAction, VerifiedEmailAction}
import controllers.navigation.Navigator
import forms.declaration.WarehouseIdentification

import javax.inject.Inject
import models.requests.JourneyRequest
import models.{DeclarationType, ExportsDeclaration, Mode}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.cache.ExportsCacheService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.declaration.{warehouse_identification, warehouse_identification_yesno}

import scala.concurrent.{ExecutionContext, Future}

class WarehouseIdentificationController @Inject()(
  authenticate: AuthAction,
  verifyEmail: VerifiedEmailAction,
  journeyType: JourneyAction,
  navigator: Navigator,
  override val exportsCacheService: ExportsCacheService,
  mcc: MessagesControllerComponents,
  warehouseIdentificationYesNoPage: warehouse_identification_yesno,
  warehouseIdentificationPage: warehouse_identification
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with ModelCacheable with SubmissionErrors {

  def displayPage(mode: Mode): Action[AnyContent] = (authenticate andThen verifyEmail andThen journeyType) { implicit request =>
    val frm = form().withSubmissionErrors()
    request.cacheModel.locations.warehouseIdentification match {
      case Some(data) => Ok(page(mode, frm.fill(data)))
      case _          => Ok(page(mode, frm))
    }
  }

  def saveIdentificationNumber(mode: Mode): Action[AnyContent] = (authenticate andThen verifyEmail andThen journeyType).async { implicit request =>
    form()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(page(mode, formWithErrors))),
        form => {
          updateCache(form)
            .map(_ => navigator.continueTo(mode, controllers.declaration.routes.SupervisingCustomsOfficeController.displayPage))
        }
      )
  }

  private def page(mode: Mode, form: Form[WarehouseIdentification])(implicit request: JourneyRequest[AnyContent]) = request.declarationType match {
    case DeclarationType.CLEARANCE => warehouseIdentificationYesNoPage(mode, form)
    case _                         => warehouseIdentificationPage(mode, form)
  }

  private def form()(implicit request: JourneyRequest[AnyContent]) = request.declarationType match {
    case DeclarationType.CLEARANCE => WarehouseIdentification.form(yesNo = true)
    case _                         => WarehouseIdentification.form(yesNo = false)
  }

  private def updateCache(formData: WarehouseIdentification)(implicit request: JourneyRequest[AnyContent]): Future[Option[ExportsDeclaration]] =
    updateExportsDeclarationSyncDirect(model => model.copy(locations = model.locations.copy(warehouseIdentification = Some(formData))))
}
