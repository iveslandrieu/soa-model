/* Copyright 2012 predic8 GmbH, www.predic8.com
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License. */

package com.predic8.schema.diff

import java.util.List;

import com.predic8.schema.ComplexType;
import com.predic8.soamodel.*

import org.apache.commons.logging.*

class ElementDiffGenerator extends UnitDiffGenerator {

	private Log log = LogFactory.getLog(this.class)

	public ElementDiffGenerator() {
		updateLabels()
	}

	def labelElementRemoved, labelElementAdded, labelElement,  labelHasChanged, labelTypeElement, labelTo,
	labelEmbeddedStandAlone, labelEmbedded, labelAttributeMinOccurs, labelAttributeMaxOccurs, labelFrom

	def removed = {new Difference(description:"${labelElementRemoved}.", type: 'element', breaks: a.exchange? true : false, exchange: a.exchange)}

	def added = { new Difference(description:"${labelElementAdded}.", type: 'element', breaks: b.exchange? true : false, exchange: b.exchange)}

	def changed = { diffs ->
		new Difference(description:"${labelElement} ${a.name ?: 'ref to ' + a.refValue}:" , type: 'element' ,  diffs : diffs, exchange: a.exchange)
	}

	protected getTypeAndName() {
		"element ${a.name}"
	}

	List<Difference> compareUnit(){
		log.debug("compareElement")
		def lDiffs = []
		lDiffs.addAll(generator.compareAnnotation(a.annotation, b.annotation))
		lDiffs.addAll(compareType())
		lDiffs.addAll(compareMinMaxOccurs())
		if(generator.compare4WSDL) lDiffs.addAll(compare4WSDL())
		lDiffs
	}

	protected List<Difference> compareType(){
		if(a.embeddedType && b.embeddedType) return compareEmbeddedType()
		if(a.embeddedType && b.type) return [
				new Difference(description:"${labelTypeElement} '${a.name}' ${labelHasChanged} ${labelEmbeddedStandAlone}.", type: 'element', warning: true, exchange: a.exchange)
			]
		if(a.type && b.embeddedType) return [
				new Difference(description:"${labelTypeElement} '${a.name}' ${labelHasChanged} ${labelEmbedded}.", type: 'element', warning: true, exchange: a.exchange)
			]
		if (!b.metaClass.hasProperty(b, 'type')) return [
				new Difference(description: "${labelTypeElement} '${a.name}' ${labelHasChanged} ${labelFrom} ${a.schema.getPrefix(a.type?.namespaceURI)?:'xsd'}:${a.type?.localPart} ${labelTo} ${b.toString()}.",
				type: 'element', breaks: true, exchange: a.exchange)
			]
		if(a.type != b.type) return [
				new Difference(description:"${labelTypeElement} '${a.name}' ${labelHasChanged} ${labelFrom} ${a.schema.getPrefix(a.type.namespaceURI)?:'xsd'}:${a.type.localPart} ${labelTo} ${b.schema.getPrefix(b.type.namespaceURI)?:'xsd'}:${b.type.localPart}.",
				type: 'element', breaks:true, exchange: a.exchange)
			]
		[]
	}

	protected List<Difference> compareMinMaxOccurs(eType = 'element'){
		def lDiffs = []
		def nameOrRef = (eType == 'any') ? 'any' : "element ${a.name ?: 'ref to ' + a.refValue}"
		if(a.minOccurs != b.minOccurs){
			lDiffs << new Difference(description: "${labelAttributeMinOccurs} $nameOrRef ${labelHasChanged} ${labelFrom} ${a.minOccurs} ${labelTo} ${b.minOccurs}.",
				 type: eType, safe:  a.minOccurs >= b.minOccurs, warning: true)
		}
		if(a.maxOccurs != b.maxOccurs){
			lDiffs << new Difference(description:"${labelAttributeMaxOccurs} $nameOrRef ${labelHasChanged} ${labelFrom} ${a.maxOccurs} ${labelTo} ${b.maxOccurs}.", type: eType, safe:  a.maxOccurs <= b.maxOccurs, warning: true)
		}
		lDiffs
	}

	//Recursion is possible if an element in the embedded type references a complexType, which returns to the wrapping element of the embedded type.
	private List<Difference> compareEmbeddedType() {
		a.embeddedType?.exchange.addAll(a.exchange)
		b.embeddedType?.exchange.addAll(b.exchange)
		a.embeddedType?.compare(generator, b.embeddedType, ctx.clone()) ?: []
	}

	//Recursion is possible if an element references a type, which returns to the element in further steps.
	List<Difference> compare4WSDL() {
		def diffs = []
		if(a.type && a.type == b.type){
			def aT = a.schema.getType(a.type)
			def bT = b.schema.getType(b.type)
			if(aT && bT){
				//exchange marks the message direction in the using wsdl:operation
				aT.exchange.addAll(a.exchange)
				bT.exchange.addAll(b.exchange)
				diffs.addAll(aT.compare(generator, bT, ctx.clone()))
			}

		}
		if(a.ref && a.ref == b.ref) {
			diffs.addAll(a.schema.getElement(a.ref).compare(generator, b.schema.getElement(b.ref), ctx.clone()))
		}
		diffs
	}

	protected def updateLabels(){
		labelElementRemoved = bundle.getString("com.predic8.schema.diff.labelRemoved")
		labelElement = bundle.getString("com.predic8.schema.diff.labelElement")
		labelElementAdded = bundle.getString("com.predic8.schema.diff.labelElementAdded")
		labelTypeElement = bundle.getString("com.predic8.schema.diff.labelTypeElement")
		labelHasChanged = bundle.getString("com.predic8.schema.diff.labelHasChanged")
		labelTypeElement = bundle.getString("com.predic8.schema.diff.labelTypeElement")
		labelEmbeddedStandAlone = bundle.getString("com.predic8.schema.diff.labelEmbeddedStandAlone")
		labelEmbedded = bundle.getString("com.predic8.schema.diff.labelEmbedded")
		labelAttributeMinOccurs = bundle.getString("com.predic8.schema.diff.labelAttributeMinOccurs")
		labelAttributeMaxOccurs = bundle.getString("com.predic8.schema.diff.labelAttributeMaxOccurs")
		labelTo = bundle.getString("com.predic8.schema.diff.labelTo")
		labelFrom = bundle.getString("com.predic8.schema.diff.labelFrom")

	}

}

