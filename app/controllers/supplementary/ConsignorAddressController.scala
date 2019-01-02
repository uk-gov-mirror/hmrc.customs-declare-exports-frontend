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
import forms.supplementary.AddressAndIdentification
import javax.inject.Inject
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.CustomsCacheService
import services.Countries.allCountries
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.supplementary.consignor_details

import scala.concurrent.Future

class ConsignorAddressController @Inject()(
  appConfig: AppConfig,
  override val messagesApi: MessagesApi,
  authenticate: AuthAction,
  customsCacheService: CustomsCacheService
) extends FrontendController with I18nSupport {

  val formId = "ConsignorAddress"
  val form = Form(AddressAndIdentification.addressMapping)

  implicit val countries = services.Countries.allCountries

  def displayForm(): Action[AnyContent] = authenticate.async { implicit request =>
    customsCacheService.fetchAndGetEntry[AddressAndIdentification](appConfig.appName, formId).map {
      case Some(data) => Ok(consignor_details(appConfig, form.fill(data)))
      case _          => Ok(consignor_details(appConfig, form))
    }
  }

  def saveAddress(): Action[AnyContent] = authenticate.async { implicit request =>
    form.bindFromRequest().fold(
      (formWithErrors: Form[AddressAndIdentification]) =>
        Future.successful(BadRequest(consignor_details(appConfig, formWithErrors))),
      form =>
        customsCacheService.cache[AddressAndIdentification](appConfig.appName, formId, form).map { _ =>
          Redirect(controllers.supplementary.routes.DeclarantAddressController.displayForm())
        }
    )
  }
}
