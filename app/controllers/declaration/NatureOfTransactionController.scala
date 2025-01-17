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
import forms.declaration.NatureOfTransaction
import forms.declaration.NatureOfTransaction._

import javax.inject.Inject
import models.requests.JourneyRequest
import models.{ExportsDeclaration, Mode}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.cache.ExportsCacheService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.declaration.nature_of_transaction

import scala.concurrent.{ExecutionContext, Future}

class NatureOfTransactionController @Inject()(
  authenticate: AuthAction,
  verifyEmail: VerifiedEmailAction,
  journeyType: JourneyAction,
  navigator: Navigator,
  mcc: MessagesControllerComponents,
  natureOfTransactionPage: nature_of_transaction,
  override val exportsCacheService: ExportsCacheService
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with ModelCacheable with SubmissionErrors {

  def displayPage(mode: Mode): Action[AnyContent] = (authenticate andThen verifyEmail andThen journeyType) { implicit request =>
    val frm = form().withSubmissionErrors()
    request.cacheModel.natureOfTransaction match {
      case Some(data) => Ok(natureOfTransactionPage(mode, frm.fill(data)))
      case _          => Ok(natureOfTransactionPage(mode, frm))
    }
  }

  def saveTransactionType(mode: Mode): Action[AnyContent] = (authenticate andThen verifyEmail andThen journeyType).async { implicit request =>
    form().bindFromRequest
      .fold(
        (formWithErrors: Form[NatureOfTransaction]) => Future.successful(BadRequest(natureOfTransactionPage(mode, formWithErrors))),
        form => updateCache(form).map(_ => navigator.continueTo(mode, controllers.declaration.routes.PreviousDocumentsSummaryController.displayPage))
      )
  }

  private def updateCache(formData: NatureOfTransaction)(implicit r: JourneyRequest[AnyContent]): Future[Option[ExportsDeclaration]] =
    updateExportsDeclarationSyncDirect(model => model.copy(natureOfTransaction = Some(formData)))
}
