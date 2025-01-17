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
import forms.declaration.DepartureTransport

import javax.inject.Inject
import models.requests.JourneyRequest
import models.{DeclarationType, ExportsDeclaration, Mode}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.cache.ExportsCacheService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.declaration.departure_transport

import scala.concurrent.{ExecutionContext, Future}

class DepartureTransportController @Inject()(
  authenticate: AuthAction,
  verifyEmail: VerifiedEmailAction,
  journeyType: JourneyAction,
  override val exportsCacheService: ExportsCacheService,
  navigator: Navigator,
  mcc: MessagesControllerComponents,
  departureTransportPage: departure_transport
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with ModelCacheable with SubmissionErrors {

  private def form()(implicit request: JourneyRequest[_]) = DepartureTransport.form(request.declarationType)

  private val validTypes = Seq(DeclarationType.STANDARD, DeclarationType.SUPPLEMENTARY, DeclarationType.CLEARANCE)

  def displayPage(mode: Mode): Action[AnyContent] =
    (authenticate andThen verifyEmail andThen journeyType(validTypes)) { implicit request =>
      val frm = form().withSubmissionErrors()
      val transport = request.cacheModel.transport
      val formData = DepartureTransport(transport.meansOfTransportOnDepartureType, transport.meansOfTransportOnDepartureIDNumber)

      Ok(departureTransportPage(mode, frm.fill(formData)))
    }

  def submitForm(mode: Mode): Action[AnyContent] =
    (authenticate andThen verifyEmail andThen journeyType(validTypes)).async { implicit request =>
      form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[DepartureTransport]) => Future.successful(BadRequest(departureTransportPage(mode, formWithErrors))),
          borderTransport =>
            updateCache(borderTransport)
              .map(_ => nextPage(mode))
        )
    }

  private def nextPage(mode: Mode)(implicit request: JourneyRequest[AnyContent]): Result =
    request.declarationType match {
      case DeclarationType.CLEARANCE =>
        navigator.continueTo(mode, controllers.declaration.routes.TransportPaymentController.displayPage)
      case DeclarationType.STANDARD | DeclarationType.SUPPLEMENTARY =>
        navigator.continueTo(mode, controllers.declaration.routes.BorderTransportController.displayPage)
    }

  private def updateCache(formData: DepartureTransport)(implicit r: JourneyRequest[AnyContent]): Future[Option[ExportsDeclaration]] =
    updateExportsDeclarationSyncDirect(_.updateDepartureTransport(formData))
}
