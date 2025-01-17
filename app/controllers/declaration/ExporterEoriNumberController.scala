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
import forms.common.YesNoAnswer.YesNoAnswers
import forms.declaration.exporter.{ExporterDetails, ExporterEoriNumber}

import javax.inject.Inject
import models.requests.JourneyRequest
import models.{DeclarationType, ExportsDeclaration, Mode}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.cache.ExportsCacheService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.declaration.exporter_eori_number

import scala.concurrent.{ExecutionContext, Future}

class ExporterEoriNumberController @Inject()(
  authenticate: AuthAction,
  verifyEmail: VerifiedEmailAction,
  journeyType: JourneyAction,
  navigator: Navigator,
  mcc: MessagesControllerComponents,
  exporterEoriDetailsPage: exporter_eori_number,
  override val exportsCacheService: ExportsCacheService
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with ModelCacheable with SubmissionErrors {

  def displayPage(mode: Mode): Action[AnyContent] = (authenticate andThen verifyEmail andThen journeyType) { implicit request =>
    val frm = ExporterEoriNumber.form().withSubmissionErrors()
    request.cacheModel.parties.exporterDetails match {
      case Some(data) => Ok(exporterEoriDetailsPage(mode, frm.fill(ExporterEoriNumber(data))))
      case _          => Ok(exporterEoriDetailsPage(mode, frm))
    }
  }

  def submit(mode: Mode): Action[AnyContent] = (authenticate andThen verifyEmail andThen journeyType).async { implicit request =>
    ExporterEoriNumber
      .form()
      .bindFromRequest()
      .fold(
        (formWithErrors: Form[ExporterEoriNumber]) => {
          val formWithAdjustedErrors = formWithErrors

          Future.successful(BadRequest(exporterEoriDetailsPage(mode, formWithAdjustedErrors)))
        },
        form =>
          updateCache(form, request.cacheModel.parties.exporterDetails)
            .map(_ => navigator.continueTo(mode, nextPage(form.hasEori)))
      )
  }

  private def nextPage(hasEori: String)(implicit request: JourneyRequest[_]): Mode => Call =
    if (hasEori == YesNoAnswers.no) {
      controllers.declaration.routes.ExporterDetailsController.displayPage
    } else {
      request.declarationType match {
        case DeclarationType.CLEARANCE => controllers.declaration.routes.IsExsController.displayPage
        case _                         => controllers.declaration.routes.RepresentativeAgentController.displayPage
      }
    }

  private def updateCache(formData: ExporterEoriNumber, savedExporterDetails: Option[ExporterDetails])(
    implicit r: JourneyRequest[AnyContent]
  ): Future[Option[ExportsDeclaration]] =
    updateExportsDeclarationSyncDirect(
      model => model.copy(parties = model.parties.copy(exporterDetails = Some(ExporterDetails.from(formData, savedExporterDetails))))
    )
}
