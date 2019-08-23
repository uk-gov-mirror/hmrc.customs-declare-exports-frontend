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

import controllers.actions.{AuthAction, JourneyAction}
import controllers.navigation.Navigator
import controllers.util._
import forms.declaration.ItemType
import forms.declaration.ItemType._
import handlers.ErrorHandler
import javax.inject.Inject
import models.ExportsDeclaration
import models.requests.JourneyRequest
import play.api.data.{Form, FormError}
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.cache.ExportsCacheService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.validators.forms.supplementary.ItemTypeValidator
import utils.validators.forms.{Invalid, Valid}
import views.html.declaration.item_type

import scala.concurrent.{ExecutionContext, Future}

class ItemTypeController @Inject()(
  authenticate: AuthAction,
  journeyType: JourneyAction,
  errorHandler: ErrorHandler,
  override val exportsCacheService: ExportsCacheService,
  navigator: Navigator,
  mcc: MessagesControllerComponents,
  itemTypePage: item_type
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with I18nSupport with ModelCacheable {

  def displayPage(itemId: String): Action[AnyContent] = (authenticate andThen journeyType) { implicit request =>
    request.cacheModel
      .itemBy(itemId)
      .map { item =>
        item.itemType match {
          case Some(itemType) =>
            Ok(
              itemTypePage(
                itemId,
                ItemType.form().fill(itemType),
                item.hasFiscalReferences,
                itemType.taricAdditionalCodes,
                itemType.nationalAdditionalCodes
              )
            )
          case None =>
            Ok(itemTypePage(itemId, ItemType.form(), item.hasFiscalReferences))
        }
      }
      .getOrElse(Redirect(routes.ItemsSummaryController.displayPage()))
  }

  def submitItemType(itemId: String): Action[AnyContent] = (authenticate andThen journeyType).async {
    implicit request =>
      val inputForm = ItemType.form().bindFromRequest()
      val itemTypeInput: ItemType = inputForm.value.getOrElse(ItemType.empty)

      request.cacheModel
        .itemBy(itemId)
        .map { item =>
          val itemTypeCache = item.itemType.getOrElse(ItemType.empty)
          val hasFiscalReferences = item.hasFiscalReferences
          val formAction = FormAction.bindFromRequest()
          formAction match {
            case Some(Add) =>
              handleAddition(itemId, itemTypeInput, itemTypeCache, hasFiscalReferences)
            case Some(SaveAndContinue) | Some(SaveAndReturn) =>
              handleSaveAndContinue(itemId, itemTypeInput, itemTypeCache, hasFiscalReferences)
            case Some(Remove(keys)) =>
              handleRemoval(itemId, keys, itemTypeCache, hasFiscalReferences)
            case _ =>
              errorHandler.displayErrorPage()
          }
        }
        .getOrElse(errorHandler.displayErrorPage())
  }

  private def handleAddition(
    itemId: String,
    itemTypeInput: ItemType,
    itemTypeCache: ItemType,
    hasFiscalReferences: Boolean
  )(implicit request: JourneyRequest[AnyContent]): Future[Result] = {
    val itemTypeUpdated = updateCachedItemTypeAddition(itemId, itemTypeInput, itemTypeCache)
    ItemTypeValidator.validateOnAddition(itemTypeUpdated) match {
      case Valid =>
        updateExportsCache(itemId, itemTypeUpdated).map {
          case Some(model) =>
            refreshPage(itemId, itemTypeInput, model)
          case None =>
            Redirect(routes.ItemsSummaryController.displayPage())
        }
      case Invalid(errors) =>
        val formWithErrors =
          errors.foldLeft(ItemType.form().fill(itemTypeInput))((form, error) => form.withError(adjustErrorKey(error)))
        Future.successful(
          BadRequest(
            itemTypePage(
              itemId,
              adjustDataKeys(formWithErrors),
              hasFiscalReferences,
              itemTypeCache.taricAdditionalCodes,
              itemTypeCache.nationalAdditionalCodes
            )
          )
        )
    }
  }

  private def updateCachedItemTypeAddition(itemId: String, itemTypeInput: ItemType, itemTypeCache: ItemType): ItemType =
    itemTypeCache.copy(
      taricAdditionalCodes = itemTypeCache.taricAdditionalCodes ++ itemTypeInput.taricAdditionalCodes,
      nationalAdditionalCodes = itemTypeCache.nationalAdditionalCodes ++ itemTypeInput.nationalAdditionalCodes
    )

  private def handleSaveAndContinue(
    itemId: String,
    itemTypeInput: ItemType,
    itemTypeCache: ItemType,
    hasFiscalReferences: Boolean
  )(implicit request: JourneyRequest[AnyContent]): Future[Result] = {
    val itemTypeUpdated = updateCachedItemTypeSaveAndContinue(itemTypeInput, itemTypeCache)
    ItemTypeValidator.validateOnSaveAndContinue(itemTypeUpdated) match {
      case Valid =>
        updateExportsCache(itemId, itemTypeUpdated).map { _ =>
          navigator.continueTo(controllers.declaration.routes.PackageInformationController.displayPage(itemId))
        }
      case Invalid(errors) =>
        val formWithErrors =
          errors.foldLeft(ItemType.form().fill(itemTypeInput))((form, error) => form.withError(adjustErrorKey(error)))
        Future.successful(
          BadRequest(
            itemTypePage(
              itemId,
              adjustDataKeys(formWithErrors),
              hasFiscalReferences,
              itemTypeCache.taricAdditionalCodes,
              itemTypeCache.nationalAdditionalCodes
            )
          )
        )
    }
  }

  private def updateCachedItemTypeSaveAndContinue(itemTypeInput: ItemType, itemTypeCache: ItemType): ItemType =
    ItemType(
      combinedNomenclatureCode = itemTypeInput.combinedNomenclatureCode,
      taricAdditionalCodes = itemTypeCache.taricAdditionalCodes ++ itemTypeInput.taricAdditionalCodes,
      nationalAdditionalCodes = itemTypeCache.nationalAdditionalCodes ++ itemTypeInput.nationalAdditionalCodes,
      descriptionOfGoods = itemTypeInput.descriptionOfGoods,
      cusCode = itemTypeInput.cusCode,
      unDangerousGoodsCode = itemTypeInput.unDangerousGoodsCode,
      statisticalValue = itemTypeInput.statisticalValue
    )

  private def adjustErrorKey(error: FormError): FormError =
    if (error.key.contains(taricAdditionalCodesKey) || error.key.contains(nationalAdditionalCodesKey))
      error.copy(key = error.key.replaceAll("\\[.*?\\]", "").concat("[]"))
    else
      error

  private def adjustDataKeys(form: Form[ItemType]): Form[ItemType] =
    form.copy(data = form.data.map {
      case (key, value) if key.contains(taricAdditionalCodesKey)    => (taricAdditionalCodesKey + "[]", value)
      case (key, value) if key.contains(nationalAdditionalCodesKey) => (nationalAdditionalCodesKey + "[]", value)
      case (key, value)                                             => (key, value)
    })

  private def handleRemoval(itemId: String, keys: Seq[String], itemTypeCached: ItemType, hasFiscalReferences: Boolean)(
    implicit request: JourneyRequest[AnyContent]
  ): Future[Result] = {
    val key = keys.headOption.getOrElse("")
    val label = Label(key)

    val itemTypeUpdated = label.name match {
      case `taricAdditionalCodesKey` =>
        itemTypeCached.copy(taricAdditionalCodes = removeElement(itemTypeCached.taricAdditionalCodes, label.value))
      case `nationalAdditionalCodesKey` =>
        itemTypeCached.copy(
          nationalAdditionalCodes = removeElement(itemTypeCached.nationalAdditionalCodes, label.value)
        )
    }
    updateExportsCache(itemId, itemTypeUpdated).map {
      case Some(model) =>
        val itemTypeInput: ItemType = ItemType.form().bindFromRequest().value.getOrElse(ItemType.empty)
        refreshPage(itemId, itemTypeInput, model)
      case None =>
        Redirect(routes.ItemsSummaryController.displayPage())
    }
  }

  private def removeElement(collection: Seq[String], valueToRemove: String): Seq[String] =
    MultipleItemsHelper.remove(collection, (_: String) == valueToRemove)

  private def refreshPage(itemId: String, itemTypeInput: ItemType, model: ExportsDeclaration)(
    implicit request: JourneyRequest[AnyContent]
  ): Result =
    model
      .itemBy(itemId)
      .map { item =>
        item.itemType match {
          case Some(cachedData) =>
            Ok(
              itemTypePage(
                item.id,
                ItemType.form().fill(itemTypeInput),
                item.hasFiscalReferences,
                cachedData.taricAdditionalCodes,
                cachedData.nationalAdditionalCodes
              )
            )
          case _ =>
            Ok(itemTypePage(itemId, ItemType.form(), item.hasFiscalReferences))
        }
      }
      .getOrElse(Redirect(routes.ItemsSummaryController.displayPage()))

  private case class Label(name: String, value: String)
  private object Label {

    def apply(str: String): Label = {
      val Array(name, value) = str.split("_")
      Label(name, value)
    }
  }

  private def updateExportsCache(itemId: String, updatedItem: ItemType)(
    implicit request: JourneyRequest[_]
  ): Future[Option[ExportsDeclaration]] =
    updateExportsDeclarationSync(model => {
      val itemList = model.items
        .find(item => item.id.equals(itemId))
        .map(_.copy(itemType = Some(updatedItem)))
        .fold(model.items)(model.items.filter(item => !item.id.equals(itemId)) + _)

      Some(model.copy(items = itemList))
    })
}
