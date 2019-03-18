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

package helpers

trait DeclarationHolderMessages {

  val declarationHolder: String = "supplementary.declarationHolder"
  val declarationHolders: String = "supplementary.declarationHolders"

  val title: String = declarationHolder + ".title"
  val authorisationCode: String = declarationHolder + ".authorisationCode"
  val authorisationCodeHint: String = declarationHolder + ".authorisationCode.hint"
  val authorisationCodeEmpty: String = declarationHolder + ".authorisationCode.empty"
  val authorisationCodeError: String = declarationHolder + ".authorisationCode.error"
  val maximumAmountReached: String = declarationHolders + ".maximumAmount.error"
  val duplicatedItem: String = declarationHolders + ".duplicated"
}