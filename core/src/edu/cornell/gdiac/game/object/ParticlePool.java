/*
 * ParticlePool.java
 *
 * A preallocated pool of Particles.
 *
 * The LibGDX Pool class is an example of a free-list.  When you deallocated
 * and object, it does not garbage collect it.  Instead it adds it to a list.
 * When you allocate a new object, it first checks the free list.  If there
 * is something on the list, it uses that object.  Otherwise, it will allocate
 * a new object.  So it is very useful if you delete objects at roughly the
 * same rate that you construct them.
 *
 * This implementation of Pool also preallocates.  This means that we create
 * all of the objects when the pool is created.  When a new object is allocated
 * and there is nothing on the free list, it takes it from the preallocated array.
 * If the array is exhausted, allocation will fail.
 *
 * Author: Walker M. White
 * Version: 2/24/2015
 */

package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.utils.*;
import edu.cornell.gdiac.game.object.Particle;


/**
 * Preallocated pool of Particle objects.
 *
 * When the pool is created, it creates an array of particles.  This limits
 * the maximum number of objects that you can have.  If you try to obtain a
 * new object, and none of the Particles in the array are free, you will
 * get nothing.
 */
public class ParticlePool extends Pool<Particle> {
    /** Default number of particles (low to show off how this works) */
    public static final int DEFAULT_CAPACITY = 20;

    /** The preallocated array of particles */
    private Particle[] memory;

    /** The next object in the array available for allocation */
    private int next;

    /**
     * Creates a ParticlePool with the default capacity.
     */
    public ParticlePool() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Creates a ParticlePool with the given capacity.
     *
     * @param capacity  The number of particles to preallocate
     */
    public ParticlePool(int capacity) {
        super(capacity);
        assert capacity > 0;

        // Preallocate objects
        memory = new Particle[capacity];
        for(int ii = 0; ii < capacity; ii++) {
            memory[ii] = new Particle();
        }

        next = 0;
    }

    /**
     * Allocates a new object from the Pool.
     *
     * INVARIANT: If this method is called, then the free list is empty.
     *
     * This is the lone method that you must implement to create a memory
     * pool.  This is where you "seed" the memory pool by allocating objects
     * when the free list is empty.  We preallocated, so we just return
     * the next object from the list.  But you could put a "new" in here if
     * you wanted to; in that case the free list is how you manage everything.
     *
     * @return A new particle object
     */
    public Particle newObject() {
        // Fail if we are outside the array
        if (next == memory.length) {
            return null;
        }

        // OPTION 1: Allocate from the array until we go too far
        // Then we will fail until one of these objects is freed.
        next++;
        return memory[next-1];

        // OPTION 2: Reallocate old objects if we go to far
        // Since objects are allocated in order of the array, cycling will
        // always reuse the oldest object.  This works like Photon lifetime.
        //next = (next+1) % memory.length;
        //return memory[next];
    }

}