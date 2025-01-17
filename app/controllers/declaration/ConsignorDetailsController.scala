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
import forms.declaration.consignor.ConsignorDetails

import javax.inject.Inject
import models.requests.JourneyRequest
import models.{DeclarationType, ExportsDeclaration, Mode}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import services.cache.ExportsCacheService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.declaration.consignor_details

import scala.concurrent.{ExecutionContext, Future}

class ConsignorDetailsController @Inject()(
  authenticate: AuthAction,
  verifyEmail: VerifiedEmailAction,
  journeyType: JourneyAction,
  override val exportsCacheService: ExportsCacheService,
  navigator: Navigator,
  mcc: MessagesControllerComponents,
  consignorDetailsPage: consignor_details
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with ModelCacheable with SubmissionErrors {

  val validJourneys = Seq(DeclarationType.CLEARANCE)

  def displayPage(mode: Mode): Action[AnyContent] = (authenticate andThen verifyEmail andThen journeyType(validJourneys)) { implicit request =>
    val frm = ConsignorDetails.form().withSubmissionErrors()
    request.cacheModel.parties.consignorDetails match {
      case Some(data) => Ok(consignorDetailsPage(mode, frm.fill(data)))
      case _          => Ok(consignorDetailsPage(mode, frm))
    }
  }

  def saveAddress(mode: Mode): Action[AnyContent] = (authenticate andThen verifyEmail andThen journeyType(validJourneys)).async { implicit request =>
    ConsignorDetails
      .form()
      .bindFromRequest()
      .fold(
        (formWithErrors: Form[ConsignorDetails]) => Future.successful(BadRequest(consignorDetailsPage(mode, formWithErrors))),
        form =>
          updateCache(form)
            .map(_ => navigator.continueTo(mode, nextPage()))
      )
  }

  private def nextPage()(implicit request: JourneyRequest[_]): Mode => Call =
    if (request.cacheModel.isDeclarantExporter) {
      controllers.declaration.routes.CarrierEoriNumberController.displayPage
    } else {
      controllers.declaration.routes.RepresentativeAgentController.displayPage
    }

  private def updateCache(formData: ConsignorDetails)(implicit request: JourneyRequest[AnyContent]): Future[Option[ExportsDeclaration]] =
    updateExportsDeclarationSyncDirect { model =>
      val updatedParties = model.parties.copy(consignorDetails = Some(formData))
      model.copy(parties = updatedParties)
    }
}
