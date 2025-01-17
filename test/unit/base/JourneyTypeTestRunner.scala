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

package unit.base

import models.DeclarationType.DeclarationType
import models.requests.JourneyRequest
import models.{DeclarationType, ExportsDeclaration}
import org.scalatest.WordSpec
import play.api.mvc.AnyContent
import services.cache.ExportsTestData

trait JourneyTypeTestRunner extends WordSpec with ExportsTestData {

  val simpleStandardDeclaration: ExportsDeclaration = aDeclaration(withType(DeclarationType.STANDARD))
  val simpleSupplementaryDeclaration: ExportsDeclaration = aDeclaration(withType(DeclarationType.SUPPLEMENTARY))
  val simpleSimplifiedDeclaration: ExportsDeclaration = aDeclaration(withType(DeclarationType.SIMPLIFIED))
  val simpleOccasionalDeclaration: ExportsDeclaration = aDeclaration(withType(DeclarationType.OCCASIONAL))
  val simpleClearanceDeclaration: ExportsDeclaration = aDeclaration(withType(DeclarationType.CLEARANCE))

  val standardRequest: JourneyRequest[AnyContent] = journeyRequest(DeclarationType.STANDARD)
  val supplementaryRequest: JourneyRequest[AnyContent] = journeyRequest(DeclarationType.SUPPLEMENTARY)
  val simplifiedRequest: JourneyRequest[AnyContent] = journeyRequest(DeclarationType.SIMPLIFIED)
  val occasionalRequest: JourneyRequest[AnyContent] = journeyRequest(DeclarationType.OCCASIONAL)
  val clearanceRequest: JourneyRequest[AnyContent] = journeyRequest(DeclarationType.CLEARANCE)

  def onEveryDeclarationJourney(modifiers: ExportsDeclarationModifier*)(f: JourneyRequest[_] => Unit): Unit =
    onJourney(DeclarationType.values.toSeq: _*)(aDeclaration(modifiers: _*))(f)

  def onJourney(types: DeclarationType*) = new JourneyRunner(types: _*)

  class JourneyRunner(types: DeclarationType*) {

    def apply(f: JourneyRequest[_] => Unit): Unit = apply(aDeclaration())(f)

    def apply(declaration: ExportsDeclaration)(f: JourneyRequest[_] => Unit): Unit = {
      if (types.isEmpty) {
        throw new RuntimeException("Attempt to test against no types - please provide at least one declaration type")
      }
      types.foreach {
        case kind @ DeclarationType.STANDARD      => onStandard(aDeclarationAfter(declaration, withType(kind)))(f)
        case kind @ DeclarationType.SUPPLEMENTARY => onSupplementary(aDeclarationAfter(declaration, withType(kind)))(f)
        case kind @ DeclarationType.SIMPLIFIED    => onSimplified(aDeclarationAfter(declaration, withType(kind)))(f)
        case kind @ DeclarationType.OCCASIONAL    => onOccasional(aDeclarationAfter(declaration, withType(kind)))(f)
        case kind @ DeclarationType.CLEARANCE     => onClearance(aDeclarationAfter(declaration, withType(kind)))(f)
        case _                                    => throw new RuntimeException("Unrecognized declaration type - you could have to implement helper methods")
      }
    }
  }

  def onStandard(f: JourneyRequest[_] => Unit): Unit =
    onStandard(simpleStandardDeclaration)(f)

  private def onStandard(declaration: ExportsDeclaration)(f: JourneyRequest[_] => Unit): Unit =
    "on Standard journey" when {
      f(journeyRequest(declaration))
    }

  def onSimplified(f: JourneyRequest[_] => Unit): Unit =
    onSimplified(simpleSimplifiedDeclaration)(f)

  private def onSimplified(declaration: ExportsDeclaration)(f: JourneyRequest[_] => Unit): Unit =
    "on Simplified journey" when {
      f(journeyRequest(declaration))
    }

  def onSupplementary(f: JourneyRequest[_] => Unit): Unit =
    onSupplementary(simpleSupplementaryDeclaration)(f)

  private def onSupplementary(declaration: ExportsDeclaration)(f: JourneyRequest[_] => Unit): Unit =
    "on Supplementary journey" when {
      f(journeyRequest(declaration))
    }

  def onOccasional(f: JourneyRequest[_] => Unit): Unit =
    onOccasional(simpleOccasionalDeclaration)(f)

  private def onOccasional(declaration: ExportsDeclaration)(f: JourneyRequest[_] => Unit): Unit =
    "on Occasional journey" when {
      f(journeyRequest(declaration))
    }

  def onClearance(f: JourneyRequest[_] => Unit): Unit =
    onClearance(simpleClearanceDeclaration)(f)

  private def onClearance(declaration: ExportsDeclaration)(f: JourneyRequest[_] => Unit): Unit =
    "on Clearance journey" when {
      f(journeyRequest(declaration))
    }
}
