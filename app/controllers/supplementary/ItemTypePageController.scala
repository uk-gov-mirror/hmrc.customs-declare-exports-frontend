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
import controllers.util.CacheIdGenerator.supplementaryCacheId
import forms.supplementary.ItemType
import forms.supplementary.validators.{Failure, ItemTypeValidator, Success, ValidationResult}
import handlers.ErrorHandler
import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.CustomsCacheService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.supplementary.item_type

import scala.concurrent.{ExecutionContext, Future}

class ItemTypePageController @Inject()(
  appConfig: AppConfig,
  override val messagesApi: MessagesApi,
  authenticate: AuthAction,
  errorHandler: ErrorHandler,
  customsCacheService: CustomsCacheService
)(implicit ec: ExecutionContext)
    extends FrontendController with I18nSupport {

  def displayPage(): Action[AnyContent] = authenticate.async { implicit request =>
    customsCacheService.fetchAndGetEntry[ItemType](supplementaryCacheId, ItemType.id).map {
      case Some(data) => Ok(item_type(appConfig, ItemType.form.fill(data)))
      case _          => Ok(item_type(appConfig, ItemType.form))
    }
  }

  // TODO: Adds duplicates to the list

  def submitItemType(): Action[AnyContent] = authenticate.async { implicit request =>
    val inputForm = ItemType.form.bindFromRequest()
    val itemTypeInput: ItemType = inputForm.value.getOrElse(ItemType.empty)

    customsCacheService.fetchAndGetEntry[ItemType](supplementaryCacheId, ItemType.id).flatMap { itemTypeCache =>
      val itemTypeUpdated = itemTypeCache.getOrElse(ItemType.empty).updateWith(itemTypeInput)
      validateMergedItemType(itemTypeUpdated).flatMap {

        case Failure(errors) =>
          val formWithErrors = errors.foldLeft(inputForm)((form, error) => form.withError(error))

          Future.successful(BadRequest(item_type(appConfig, formWithErrors)))

        case Success =>
          customsCacheService.cache[ItemType](supplementaryCacheId, ItemType.id, itemTypeUpdated).map { _ =>
            Redirect(controllers.supplementary.routes.PackageInformationController.displayForm())
          }

      }
    }
  }

//  private def handleAddition() = {
//    val itemTypeUpdated = updateCachedItemTypeAddition
//
//  }
//
//  private def handleSaveAndContinue() = {
//
//  }



  private def updateCachedItemTypeAddition(itemTypeInput: ItemType, itemTypeCache: ItemType): ItemType =
    itemTypeCache.copy(
      taricAdditionalCodes = itemTypeCache.taricAdditionalCodes ++ itemTypeInput.taricAdditionalCodes,
      nationalAdditionalCodes = itemTypeCache.nationalAdditionalCodes ++ itemTypeInput.nationalAdditionalCodes
    )

  private def updateCachedItemTypeSaveAndContinue(itemTypeInput: ItemType, itemTypeCache: ItemType): ItemType =
    ItemType(
      combinedNomenclatureCode = itemTypeInput.combinedNomenclatureCode,
      taricAdditionalCodes = itemTypeCache.taricAdditionalCodes ++ itemTypeInput.taricAdditionalCodes,
      nationalAdditionalCodes = itemTypeCache.nationalAdditionalCodes ++ itemTypeInput.nationalAdditionalCodes,
      descriptionOfGoods = itemTypeInput.descriptionOfGoods,
      cusCode = itemTypeInput.cusCode,
      statisticalValue = itemTypeInput.statisticalValue
    )

  private def validateMergedItemType(itemType: ItemType): Future[ValidationResult] =
    Future.successful(ItemTypeValidator.validateOnSaveAndContinue(itemType))

}