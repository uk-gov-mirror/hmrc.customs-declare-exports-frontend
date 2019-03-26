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

package controllers.declaration

import config.AppConfig
import controllers.actions.{AuthAction, JourneyAction}
import controllers.util.CacheIdGenerator.cacheId
import forms.declaration.DispatchLocation.AllowedDispatchLocations
import forms.declaration.{AdditionalDeclarationType, DispatchLocation}
import handlers.ErrorHandler
import javax.inject.Inject
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call}
import services.CustomsCacheService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.declaration.{declaration_type, dispatch_location}

import scala.concurrent.{ExecutionContext, Future}

class DeclarationTypeController @Inject()(
  appConfig: AppConfig,
  override val messagesApi: MessagesApi,
  authenticate: AuthAction,
  journeyType: JourneyAction,
  errorHandler: ErrorHandler,
  customsCacheService: CustomsCacheService
)(implicit ec: ExecutionContext)
    extends FrontendController with I18nSupport {

  def displayDispatchLocationPage(): Action[AnyContent] = (authenticate andThen journeyType).async { implicit request =>
    customsCacheService
      .fetchAndGetEntry[DispatchLocation](cacheId, DispatchLocation.formId)
      .map {
        case Some(data) => Ok(dispatch_location(appConfig, DispatchLocation.form().fill(data)))
        case _          => Ok(dispatch_location(appConfig, DispatchLocation.form()))
      }
  }

  def submitDispatchLocation(): Action[AnyContent] = (authenticate andThen journeyType).async { implicit request =>
    DispatchLocation
      .form()
      .bindFromRequest()
      .fold(
        (formWithErrors: Form[DispatchLocation]) =>
          Future.successful(BadRequest(dispatch_location(appConfig, formWithErrors))),
        validDispatchLocation =>
          customsCacheService
            .cache[DispatchLocation](cacheId, DispatchLocation.formId, validDispatchLocation)
            .map { _ =>
              Redirect(specifyNextPage(validDispatchLocation))
          }
      )
  }

  private def specifyNextPage(providedDispatchLocation: DispatchLocation): Call =
    providedDispatchLocation.dispatchLocation match {
      case AllowedDispatchLocations.OutsideEU =>
        controllers.declaration.routes.DeclarationTypeController.displayAdditionalDeclarationTypePage()
      case AllowedDispatchLocations.SpecialFiscalTerritory =>
        controllers.declaration.routes.NotEligibleController.displayPage()
    }

  def displayAdditionalDeclarationTypePage(): Action[AnyContent] = (authenticate andThen journeyType).async {
    implicit request =>
      customsCacheService
        .fetchAndGetEntry[AdditionalDeclarationType](cacheId, AdditionalDeclarationType.formId)
        .map {
          case Some(data) => Ok(declaration_type(appConfig, AdditionalDeclarationType.form().fill(data)))
          case _          => Ok(declaration_type(appConfig, AdditionalDeclarationType.form()))
        }
  }

  def submitAdditionalDeclarationType(): Action[AnyContent] = (authenticate andThen journeyType).async {
    implicit request =>
      AdditionalDeclarationType
        .form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[AdditionalDeclarationType]) =>
            Future.successful(BadRequest(declaration_type(appConfig, formWithErrors))),
          validAdditionalDeclarationType =>
            customsCacheService
              .cache[AdditionalDeclarationType](
                cacheId,
                AdditionalDeclarationType.formId,
                validAdditionalDeclarationType
              )
              .map(_ => Redirect(controllers.declaration.routes.ConsignmentReferencesController.displayPage()))
        )
  }
}
