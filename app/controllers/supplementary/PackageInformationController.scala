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

package controllers.supplementary

import config.AppConfig
import controllers.actions.AuthAction
import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import services.CustomsCacheService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import forms.supplementary.PackageInformation
import play.api.data.Form
import play.api.mvc.{Action, AnyContent}
import views.html.supplementary.package_information

import scala.concurrent.{ExecutionContext, Future}

class PackageInformationController @Inject()(
  appConfig: AppConfig,
  override val messagesApi: MessagesApi,
  authenticate: AuthAction,
  customsCacheService: CustomsCacheService
)(implicit ec: ExecutionContext)
    extends FrontendController with I18nSupport {
  import forms.supplementary.PackageInformation._

  def displayForm(): Action[AnyContent] = authenticate.async { implicit request =>
    customsCacheService.fetchAndGetEntry[PackageInformation](appConfig.appName, formId).map {
      case Some(data) => Ok(package_information(appConfig, form.fill(data)))
      case _          => Ok(package_information(appConfig, form))
    }
  }

  def submit(): Action[AnyContent] = authenticate.async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        (formWithErrors: Form[PackageInformation]) =>
          Future.successful(BadRequest(package_information(appConfig, formWithErrors))),
        form =>
          customsCacheService.cache[PackageInformation](appConfig.appName, formId, form).map { _ =>
            Redirect(controllers.supplementary.routes.PreviousDocumentsController.displayForm())
        }
      )
  }
}