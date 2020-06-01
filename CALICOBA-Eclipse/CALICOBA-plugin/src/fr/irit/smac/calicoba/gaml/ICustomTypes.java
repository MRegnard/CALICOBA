package fr.irit.smac.calicoba.gaml;

import msi.gaml.types.IType;

/**
 * Interface declaring all custom types and their ID.
 * 
 * @author Damien Vergnet
 */
public interface ICustomTypes {
  String PARAMETER_CONTEXT = "parameter_context";
  int PARAMETER_CONTEXT_ID = IType.AVAILABLE_TYPES;

  String PARAMETER_MEMORY_ENTRY = "parameter_memory_entry";
  int PARAMETER_MEMORY_ENTRY_ID = IType.AVAILABLE_TYPES + 1;

  String TRIPLET = "triplet";
  int TRIPLET_ID = IType.AVAILABLE_TYPES + 2;
}
