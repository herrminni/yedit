package org.dadacoalition.yedit.editor;

import java.util.ArrayList;
import java.util.List;

import org.dadacoalition.yedit.YEditLog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

/**
 * This class is used to represent the elements in the outline view.
 * @author oysteto
 *
 */
public class YAMLOutlineElement {
		
        /** The element is a single scalar. */
		public static final int SCALAR = 1;
		
		/** The element is part of a mapping */
		public static final int MAPPINGITEM = 2;
		
		/** The element is part of a sequence */
		public static final int SEQUENCEITEM = 3;
		
		/** The element is a document element */
		public static final int DOCUMENT = 4;
		
		/** The element is a key value pair where the value is a single scalar */
		public static final int MAPPINGSCALAR = 5;
	
		protected Node node;
		protected String key;
		protected String value;
		protected YAMLOutlineElement parent;
		protected List<YAMLOutlineElement> children = new ArrayList<YAMLOutlineElement>();
		protected Position position;
		protected IDocument document;
		protected int type;
		
		/**
		 * Create an DOCUMENT element. The document elements are the roots in the
		 * outline tree
		 * @param node A node as returned by SnakeYAML.
		 * @param document The document containing the YAML text
		 */
		protected YAMLOutlineElement( Node node, IDocument document ){
			this( node, null, DOCUMENT, document );		
		}		
		
		/**
		 * @param node A node as returned by SnakeYAML
		 * @param parent The parent element
		 * @param type The type of YAML element that this is. The different element types are defined
		 * as constants in this class.
		 * @param document The document containing the YAML text.
		 */
		protected YAMLOutlineElement( Node node, YAMLOutlineElement parent, int type, IDocument document ){
			this.node = node;
			this.parent = parent;
			this.type = type;
			this.document = document;
			
			parseNode( node );
			position = getPosition( node );
			try {
				document.addPosition(YAMLContentOutlinePage.YAMLSEGMENT, position);
			} catch (BadLocationException e) {
			    YEditLog.logger.warning(e.toString());				
			} catch (BadPositionCategoryException e) {
				YEditLog.logger.warning(e.toString());
			}			
		}
		
		/**
		 * Get the position of the node within the document containing the YAML text.
		 * This position is necessary to associate clicking on the element in the
		 * outline view with moving the caret to the right position in the document.
		 * @param node The SnakeYAML node of the current element.
		 * @return
		 */
		private Position getPosition( Node node ){
			
			int startLine = node.getStartMark().getLine();
			int startColumn = node.getStartMark().getColumn();
			int endLine = node.getEndMark().getLine();
			int endColumn = node.getEndMark().getColumn();
			Position p = null;
			try {
				int offset = document.getLineOffset(startLine);
				offset += startColumn;
			
				int length;
				if( startLine < endLine ){
					length = document.getLineLength( startLine ) - startColumn;
				} else {
					length = endColumn - startColumn;
				}
				p = new Position( offset, length ); 
			} catch( BadLocationException e) {
				YEditLog.logger.warning( e.toString() );
			}
			return p; 			
			
		}
		
		/**
		 * Parse the node to determine what type of element it is and if
		 * it has any children. This method recursively constructs any children of
		 * the element as well.
		 * @param node The SnakeYAML of the current element.
		 */
		private void parseNode( Node node ){
			
			if( node instanceof ScalarNode ){
			    
			    //At this stage we know that the element has no children, therefore change the
			    //type of the element.
				if( this.type == SEQUENCEITEM ){
					this.type = SCALAR;
				} else if( this.type == MAPPINGITEM ){
					this.type = MAPPINGSCALAR;
				}				
			} else if( node instanceof SequenceNode ){
				SequenceNode sNode = (SequenceNode) node;
				List<Node> children = sNode.getValue();
				for( Node childNode : children ){
					YAMLOutlineElement child = new YAMLOutlineElement( childNode, this, SEQUENCEITEM, document );
					this.children.add(child);					
				}
			} else if ( node instanceof MappingNode ){
				MappingNode mNode = (MappingNode) node;
				List<Node[]> children = mNode.getValue();
				for( Node[] childNode : children ){
					String key = childNode[0].getValue().toString();
					YAMLOutlineElement child = new YAMLOutlineElement( childNode[1], this, MAPPINGITEM, document );
					child.key = key;
					this.children.add(child);
				}
			}		
			
		}
		
		public String toString(){
			if( type == YAMLOutlineElement.DOCUMENT ){
				return "";
			} else if( type == YAMLOutlineElement.SCALAR ){
				return node.getValue().toString();
			} else if( type == YAMLOutlineElement.MAPPINGITEM ){
				return key;
			} else if( type == YAMLOutlineElement.SEQUENCEITEM ){
				return "";
			} else if( type == YAMLOutlineElement.MAPPINGSCALAR ){
				return key + ": " + node.getValue().toString();
			}
			
			return super.toString();
			
		}			
	
}
