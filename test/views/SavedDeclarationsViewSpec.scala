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

package views

import java.time.{LocalDateTime, ZoneOffset}

import base.ExportsTestData
import base.ExportsTestData._
import forms.Ducr
import forms.declaration.ConsignmentReferences
import helpers.views.declaration.CommonMessages
import models.{DeclarationStatus, ExportsDeclaration, Page, Paginated}
import org.jsoup.nodes.Element
import play.twirl.api.Html
import views.declaration.spec.ViewSpec
import views.html.saved_declarations
import views.tags.ViewTest

@ViewTest
class SavedDeclarationsViewSpec extends ViewSpec with CommonMessages {

  val title: String = "saved.declarations.title"
  val ducr: String = "saved.declarations.ducr"
  val dateSaved: String = "saved.declarations.dateSaved"

  private val savedDeclarationsPage = app.injector.instanceOf[saved_declarations]
  private val decWithDucr = ExportsTestData.aDeclaration(
    withStatus(DeclarationStatus.DRAFT),
    withConsignmentReferences(ConsignmentReferences(Ducr("DUCR-12345"), "LRN-1234")),
    withUpdateTime(LocalDateTime.of(2019, 1, 1, 10, 0, 0).toInstant(ZoneOffset.UTC))
  )
  private val decWithoutDucr = ExportsTestData.aDeclaration(
    withStatus(DeclarationStatus.DRAFT),
    withUpdateTime(LocalDateTime.of(2019, 1, 1, 9, 45, 0).toInstant(ZoneOffset.UTC))
  )
  private def createView(
    data: Paginated[ExportsDeclaration] = Paginated(Seq(decWithDucr, decWithoutDucr), Page(), 0)
  ): Html =
    savedDeclarationsPage(data)

  "Saved Declarations View" should {

    "display list of declarations" in {
      val view = createView()

      getElementByCss(view, "title").text() must be(messages(title))
      tableCell(view)(0, 0).text() must be(messages(ducr))
      tableCell(view)(0, 1).text() must be(messages(dateSaved))

      tableCell(view)(1, 0).text() must be("DUCR-12345")
      tableCell(view)(1, 1).text() must be("1 Jan 2019 at 10:00")
      tableCell(view)(2, 0).text() must be("No DUCR added")
      tableCell(view)(2, 1).text() must be("1 Jan 2019 at 09:45")
    }

  }

  private def tableCell(view: Html)(row: Int, column: Int): Element =
    getElementsByCss(view, ".table-row")
      .get(row)
      .getElementsByClass("table-cell")
      .get(column)
}
