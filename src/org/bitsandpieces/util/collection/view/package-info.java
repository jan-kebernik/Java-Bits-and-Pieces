/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * Provides read-only interfaces and implementations for most {@code
 * Collection}s. The advantage over Collections.unmodifiableX() is that the
 * read-only limitation is implicit in the API design: {@code View}s cannot even
 * be attempted to be modifed (except via their {@code Iterator}s, which will
 * always fail).
 */
package org.bitsandpieces.util.collection.view;
