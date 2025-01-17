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
import forms.declaration.carrier.CarrierDetails

import javax.inject.Inject
import models.DeclarationType._
import models.Mode
import models.requests.JourneyRequest
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.cache.ExportsCacheService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.declaration.carrier_details

import scala.concurrent.{ExecutionContext, Future}

class CarrierDetailsController @Inject()(
  authenticate: AuthAction,
  verifyEmail: VerifiedEmailAction,
  journeyType: JourneyAction,
  override val exportsCacheService: ExportsCacheService,
  navigator: Navigator,
  mcc: MessagesControllerComponents,
  carrierDetailsPage: carrier_details
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with ModelCacheable with SubmissionErrors {

  private val validTypes = Seq(STANDARD, SIMPLIFIED, OCCASIONAL, CLEARANCE)

  def displayPage(mode: Mode): Action[AnyContent] =
    (authenticate andThen verifyEmail andThen journeyType(validTypes)) { implicit request =>
      request.cacheModel.parties.carrierDetails match {
        case Some(data) => Ok(carrierDetailsPage(mode, form().fill(data)))
        case _          => Ok(carrierDetailsPage(mode, form()))
      }
    }

  private def form()(implicit request: JourneyRequest[_]) = CarrierDetails.form(request.declarationType).withSubmissionErrors()

  def saveAddress(mode: Mode): Action[AnyContent] =
    (authenticate andThen verifyEmail andThen journeyType(validTypes)).async { implicit request =>
      form()
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[CarrierDetails]) => Future.successful(BadRequest(carrierDetailsPage(mode, formWithErrors))),
          form =>
            updateCache(form).map { _ =>
              navigator.continueTo(mode, controllers.declaration.routes.ConsigneeDetailsController.displayPage)
          }
        )
    }

  private def updateCache(formData: CarrierDetails)(implicit req: JourneyRequest[AnyContent]) =
    updateExportsDeclarationSyncDirect(model => {
      val updatedParties = model.parties.copy(carrierDetails = Some(formData))
      model.copy(parties = updatedParties)
    })
}
