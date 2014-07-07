/*******************************************************************************
 * Caleydo - Visualization for Molecular Biology - http://caleydo.org
 * Copyright (c) The Caleydo Team. All rights reserved.
 * Licensed under the new BSD license, available at http://caleydo.org/license
 ******************************************************************************/
package org.caleydo.view.dynamicpathway.ui;

import gleem.linalg.Vec2f;

import java.util.HashSet;
import java.util.Set;

import org.caleydo.core.data.selection.EventBasedSelectionManager;
import org.caleydo.core.data.selection.IEventBasedSelectionManagerUser;
import org.caleydo.core.id.IDType;
import org.caleydo.core.view.opengl.layout2.GLGraphics;
import org.caleydo.core.view.opengl.layout2.animation.AnimatedGLElementContainer;
import org.caleydo.datadomain.genetic.EGeneIDTypes;
import org.caleydo.datadomain.pathway.graph.PathwayGraph;
import org.caleydo.datadomain.pathway.graph.item.vertex.EPathwayVertexType;
import org.caleydo.datadomain.pathway.graph.item.vertex.PathwayVertex;
import org.caleydo.datadomain.pathway.graph.item.vertex.PathwayVertexRep;
import org.caleydo.view.dynamicpathway.internal.DynamicPathwayView;
import org.caleydo.view.dynamicpathway.layout.DynamicPathwayGraph;
import org.caleydo.view.dynamicpathway.layout.GLFruchtermanReingoldLayout;
import org.caleydo.view.dynamicpathway.layout.IFRLayoutEdge;
import org.caleydo.view.dynamicpathway.layout.IFRLayoutGraph;
import org.caleydo.view.dynamicpathway.layout.IFRLayoutNode;
import org.jgrapht.graph.DefaultEdge;

/**
 * Container, which is defined by the graph layout {@link GLFruchtermanReingoldLayout} contains the renderable
 * Elements
 * 
 * @author Christiane Schwarzl
 * 
 */
public class DynamicPathwayGraphRepresentation extends AnimatedGLElementContainer implements IFRLayoutGraph, IEventBasedSelectionManagerUser {

	/**
	 * contains focus & kontextpathway informations
	 */
	private DynamicPathwayGraph pathway;

	/**
	 * contains nodes & edges used for defining and rendering the layout
	 */
	private Set<IFRLayoutNode> nodeSet;
	private Set<IFRLayoutEdge> edgeSet;


	/**
	 * the currently selected node
	 */
	private NodeElement currentSelectedNode;

	/**
	 * the view that hold the pathway list & the pathway representation
	 */
	private DynamicPathwayView view;
	
	
	private EventBasedSelectionManager geneSelectionManager;

	public DynamicPathwayGraphRepresentation(GLFruchtermanReingoldLayout layout, DynamicPathwayView view) {

		this.pathway = new DynamicPathwayGraph();

		this.nodeSet = new HashSet<IFRLayoutNode>();
		this.edgeSet = new HashSet<IFRLayoutEdge>();

		this.view = view;
		
		this.geneSelectionManager = new EventBasedSelectionManager(this, IDType.getIDType(EGeneIDTypes.PATHWAY_VERTEX.name()));


		setLayout(layout);

	}

	/**
	 * 
	 * @param graph
	 *            if a new pathway was added, a new combined (focus + parts of kontext pathways) pathway is
	 *            created
	 * 
	 */
	public void addPathwayRep(PathwayGraph graph) {

		/**
		 * if a node is selected & another pathway was selected, this has to be a kontextpathway
		 */
		Boolean addKontextPathway = (pathway.isFocusGraphSet() && (currentSelectedNode != null)) ? true
				: false;

		pathway.addFocusOrKontextPathway(graph, addKontextPathway, currentSelectedNode);
		if (addKontextPathway) {
			currentSelectedNode = null;
			view.unfilterPathwayList();
		}

		nodeSet.clear();
		edgeSet.clear();

		clear();

		for (PathwayVertexRep vrep : pathway.getCombinedVertexSet()) {

			NodeElement node;

			if (vrep.getType() == EPathwayVertexType.compound) {
				node = new NodeCompoundElement(vrep, this);
			} else if (vrep.getType() == EPathwayVertexType.group) {
				node = new NodeGroupElement(vrep, this);
			} else {
				node = new NodeGeneElement(vrep, this);

			}

			/**
			 * so the layouting algorithm can extinguish, if it's a node or an edge
			 */
			node.setLayoutData(true);

			/**
			 * needed for the edge, because edges just get you the vertexRep of the source & target vertices,
			 * but not the element, which contain the new position
			 */
			pathway.addVertexNodeMapEntry(vrep, node);

			/**
			 * needed for the layouting algorithm
			 */
			nodeSet.add((IFRLayoutNode) node);
			add(node);

		}


		for (DefaultEdge e : pathway.getCombinedEdgeSet()) {
			PathwayVertexRep vrepSource = pathway.getEdgeSource(e);
			PathwayVertexRep vrepTarget = pathway.getEdgeTarget(e);
			NodeElement nodeSource = pathway.getNodeOfVertex(vrepSource);
			NodeElement nodeTarget = pathway.getNodeOfVertex(vrepTarget);

			EdgeElement edgeElement = new EdgeElement(e, nodeSource, nodeTarget);

			/**
			 * so the layouting algorithm can extinguish, if it's a node or an edge
			 */
			edgeElement.setLayoutData(false);

			/**
			 * needed for the layouting algorithm
			 */
			edgeSet.add((IFRLayoutEdge) edgeElement);

			add(edgeElement);
		}

	}

	@Override
	protected void renderImpl(GLGraphics g, float w, float h) {
		super.renderImpl(g, w, h);
	}

	@Override
	public Vec2f getMinSize() {
		return new Vec2f(100, 100);
	}

	public DynamicPathwayGraph getDynamicPathway() {
		return pathway;
	}

	@Override
	public Set<IFRLayoutNode> getNodeSet() {
		return this.nodeSet;
	}

	@Override
	public Set<IFRLayoutEdge> getEdgeSet() {
		return this.edgeSet;
	}

	/**
	 * if a node (wrapper for PathwayVertexRep) is selected, it is highlighted and the pathway list on the
	 * left is filtered by pathways, which contain this element
	 * 
	 * @param newSelectedNode
	 */
	public void setOrResetSelectedNode(NodeElement newSelectedNode) {
		/**
		 * if nothing was selected, just set the new node
		 */
		if (currentSelectedNode == null) {
			currentSelectedNode = newSelectedNode;
			currentSelectedNode.setIsNodeSelected(true);

			view.filterPathwayList(currentSelectedNode.getVertexRep());
		}
		/**
		 * if the node was already selected, deselect it
		 */
		else if (currentSelectedNode == newSelectedNode) {
			currentSelectedNode.setIsNodeSelected(false);
			currentSelectedNode = null;

			view.unfilterPathwayList();

		}
		/**
		 * if another node was selected before, deselect it and selected the new node
		 */
		else {
			currentSelectedNode.setIsNodeSelected(false);
			currentSelectedNode = newSelectedNode;
			currentSelectedNode.setIsNodeSelected(true);

			view.filterPathwayList(currentSelectedNode.getVertexRep());
		}

	}

	public DynamicPathwayView getView() {
		return view;
	}

	@Override
	public void notifyOfSelectionChange(EventBasedSelectionManager selectionManager) {
		// TODO Auto-generated method stub
		
		System.out.println("wahhhhhh");
		
	}

}
