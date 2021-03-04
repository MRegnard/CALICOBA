package fr.irit.smac.calicoba.gaml.types;

import msi.gaml.types.IType;

/**
 * All custom types and their ID.
 * 
 * @author Damien Vergnet
 */
public interface ICustomTypes {
  String TRIPLET = "triplet";
  int TRIPLET_ID = IType.AVAILABLE_TYPES + 1;
  String OBJ_DEF = "obj_def";
  int OBJ_DEF_ID = TRIPLET_ID + 1;
}
