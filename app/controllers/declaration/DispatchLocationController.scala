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
import forms.declaration.DispatchLocation
import forms.declaration.DispatchLocation.AllowedDispatchLocations

import javax.inject.Inject
import models.requests.{ExportsSessionKeys, JourneyRequest}
import models.{ExportsDeclaration, Mode}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.cache.ExportsCacheService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.declaration.dispatch_location

import scala.concurrent.{ExecutionContext, Future}

class DispatchLocationController @Inject()(
  authenticate: AuthAction,
  verifyEmail: VerifiedEmailAction,
  journeyType: JourneyAction,
  override val exportsCacheService: ExportsCacheService,
  navigator: Navigator,
  mcc: MessagesControllerComponents,
  dispatchLocationPage: dispatch_location
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with ModelCacheable with SubmissionErrors {

  def displayPage(mode: Mode): Action[AnyContent] = (authenticate andThen verifyEmail andThen journeyType) { implicit request =>
    val frm = DispatchLocation.form().withSubmissionErrors()
    request.cacheModel.dispatchLocation match {
      case Some(data) => Ok(dispatchLocationPage(mode, frm.fill(data)))
      case _          => Ok(dispatchLocationPage(mode, frm))
    }
  }

  def submitForm(mode: Mode): Action[AnyContent] = (authenticate andThen verifyEmail andThen journeyType).async { implicit request =>
    DispatchLocation
      .form()
      .bindFromRequest()
      .fold(
        (formWithErrors: Form[DispatchLocation]) => Future.successful(BadRequest(dispatchLocationPage(mode, formWithErrors))),
        validDispatchLocation =>
          updateCache(validDispatchLocation)
            .map(
              _ =>
                validDispatchLocation.dispatchLocation match {
                  case AllowedDispatchLocations.OutsideEU =>
                    navigator.continueTo(mode, controllers.declaration.routes.AdditionalDeclarationTypeController.displayPage)
                  case AllowedDispatchLocations.SpecialFiscalTerritory =>
                    Redirect(controllers.declaration.routes.NotEligibleController.displayNotEligible())
                      .removingFromSession(ExportsSessionKeys.declarationId)
              }
          )
      )
  }

  private def updateCache(formData: DispatchLocation)(implicit request: JourneyRequest[AnyContent]): Future[Option[ExportsDeclaration]] =
    updateExportsDeclarationSyncDirect(model => model.copy(dispatchLocation = Some(formData)))

}
