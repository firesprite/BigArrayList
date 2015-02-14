
/*
 * BigArrayList
 * Copyright (C) 2015  Douglas Selent
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.dselent.bigarraylist;


/**
 * Class that manages the mapping from files on disk to elements in memory for the BigArrayList class.
 * Uses an LRU policy at the cache block level to determine which cache block should be swapped out next.
 * 
 * @author Douglas Selent
 *
 */
public class CacheMapping
{
	//should test defaults

	//must be less than Integer.MAX_Value
	/**
	 * Default size of cache block = 1,000,000
	 */
	private final int DEFAULT_BLOCK_SIZE = 1000000;
	
	/**
	 * Default number of cache blocks = 2
	 */
	private final int DEFAULT_CACHE_BLOCKS = 2;

	/**
	 * The minimum size of a cache block = 10 elements
	 */
	private final int MIN_TABLE_SIZE = 10;
	
	/**
	 * The maximum size of a cache block = the integer limit of 2^31 - 1
	 */
	private final int MAX_TABLE_SIZE = Integer.MAX_VALUE;

	/**
	 * The minimum number of cache blocks = 2
	 */
	private final int MIN_CACHE_BLOCKS = 2;
	
	/**
	 * The maximum number of cache blocks = the integer limit of 2^31 - 1
	 */
	private final int MAX_CACHE_BLOCKS = Integer.MAX_VALUE;

	/**
	 * The size of the cache blocks
	 */
	private int blockize;
	
	/**
	 * The number of cache blocks
	 */
	private int cacheBlocks;

	//stores the actual list number that the current block / element maps to
	//may not be needed?
	//private long[][] cacheTable;

	/**
	 * Array storing the next spot to add to for each cache
	 */
	private int[] cacheTableSpots;

	/**
	 * The file number each ArrayList/Cache block is currently storing
	 * Index is the same as the ArrayList index
	 */
	private int[] cacheTableFiles;

	/**
	 * Array of when each block was last used
	 * Stores a list of block numbers sorted from least recently used to most recently used
	 * most recent = end of array
	 */
	private int[] mostRecentlyUsedList;

	/**
	 * Array for each cache block for whether or not it's data has changed
	 * If clean, then it does not need to be written to disk when swapped out
	 * If dirty, then it does need to be written to disk when swapped out
	 */
	private boolean[] dirtyBits;

	/**
	 * Reference to the associated BigArrayList object
	 */
	private BigArrayList<?> bigArrayList;
	
	/**
	 * Referene to the accociated FileAccessor object
	 */
	private FileAccessor fileAccessor;

	/**
	 * Constructs a CacheMapping object for the BigArrayList
	 * @param theList Associated BigArrayList
	 */
	protected CacheMapping(BigArrayList<?> theList)
	{
		blockize = DEFAULT_BLOCK_SIZE;
		cacheBlocks = DEFAULT_CACHE_BLOCKS;

		cacheTableSpots = new int[cacheBlocks];
		cacheTableFiles = new int[cacheBlocks];
		mostRecentlyUsedList = new int[cacheBlocks];
		dirtyBits = new boolean[cacheBlocks];

		for(int i=0; i<cacheBlocks; i++)
		{
			cacheTableSpots[i] = 0;
			cacheTableFiles[i] = -1;
			mostRecentlyUsedList[i] = -1;
			dirtyBits[i] = false;
		}

		bigArrayList = theList;
		fileAccessor = new FileAccessor();
	}

	/**
	 * Constructs a CacheMapping object for the BigArrayList with the following parameters
	 * 
	 * @param newBlockSize Size of each cache block
	 * @param newCacheBlocks Number of cache blocks
	 * @param theList Associated BigArrayList
	 */
	protected CacheMapping(int newBlockSize, int newCacheBlocks, BigArrayList<?> theList)
	{
		if(newBlockSize < MIN_TABLE_SIZE || newBlockSize > MAX_TABLE_SIZE)
		{
			throw new IndexOutOfBoundsException("Table size is " + newBlockSize + " but must be <= " + MAX_TABLE_SIZE + " and >= " + MIN_TABLE_SIZE);
		}
		else
		{
			blockize = newBlockSize;
		}

		if(newCacheBlocks < MIN_CACHE_BLOCKS || newCacheBlocks > MAX_CACHE_BLOCKS)
		{
			throw new IndexOutOfBoundsException("Number of cache blocks is " + newCacheBlocks +  " but must be <= " + MAX_CACHE_BLOCKS + " and >= " + MIN_CACHE_BLOCKS);
		}
		else
		{
			cacheBlocks = newCacheBlocks;
		}
		
		cacheTableSpots = new int[cacheBlocks];
		cacheTableFiles = new int[cacheBlocks];
		mostRecentlyUsedList = new int[cacheBlocks];
		dirtyBits = new boolean[cacheBlocks];

		for(int i=0; i<cacheBlocks; i++)
		{
			cacheTableSpots[i] = 0;
			cacheTableFiles[i] = -1;
			mostRecentlyUsedList[i] = -1;
			dirtyBits[i] = false;
		}
		
		bigArrayList = theList;
		fileAccessor = new FileAccessor();
	}

	/**
	 * Constructs a CacheMapping object for the BigArrayList with the following parameters
	 * 
	 * @param memoryFilePath Folder path to where the data should be written
	 * @param theList Associated BigArrayList
	 */
	protected CacheMapping(String memoryFilePath, BigArrayList<?> theList)
	{
		blockize = DEFAULT_BLOCK_SIZE;
		cacheBlocks = DEFAULT_CACHE_BLOCKS;

		cacheTableSpots = new int[cacheBlocks];
		cacheTableFiles = new int[cacheBlocks];
		mostRecentlyUsedList = new int[cacheBlocks];
		dirtyBits = new boolean[cacheBlocks];

		for(int i=0; i<cacheBlocks; i++)
		{
			cacheTableSpots[i] = 0;
			cacheTableFiles[i] = -1;
			mostRecentlyUsedList[i] = -1;
			dirtyBits[i] = false;
		}

		bigArrayList = theList;
		fileAccessor = new FileAccessor(memoryFilePath);
	}

	/**
	 * Constructs a CacheMapping object for the BigArrayList with the following parameters
	 * 
	 * @param newBlockSize Size of each cache block
	 * @param newCacheBlocks Number of cache blocks
	 * @param memoryFilePath Folder path to where the data should be written
	 * @param theList Associated BigArrayList
	 */
	protected CacheMapping(int newBlockSize, int newCacheBlocks, String memoryFilePath, BigArrayList<?> theList)
	{
		if(newBlockSize < MIN_TABLE_SIZE || newBlockSize > MAX_TABLE_SIZE)
		{
			throw new IndexOutOfBoundsException("Table size is " + newBlockSize + " but must be <= " + MAX_TABLE_SIZE + " and >= " + MIN_TABLE_SIZE);
		}
		else
		{
			blockize = newBlockSize;
		}

		if(newCacheBlocks < MIN_CACHE_BLOCKS || newCacheBlocks > MAX_CACHE_BLOCKS)
		{
			throw new IndexOutOfBoundsException("Number of cache blocks is " + newCacheBlocks +  " but must be <= " + MAX_CACHE_BLOCKS + " and >= " + MIN_CACHE_BLOCKS);
		}
		else
		{
			cacheBlocks = newCacheBlocks;
		}
		
		cacheTableSpots = new int[cacheBlocks];
		cacheTableFiles = new int[cacheBlocks];
		mostRecentlyUsedList = new int[cacheBlocks];
		dirtyBits = new boolean[cacheBlocks];

		for(int i=0; i<cacheBlocks; i++)
		{
			cacheTableSpots[i] = 0;
			cacheTableFiles[i] = -1;
			mostRecentlyUsedList[i] = -1;
			dirtyBits[i] = false;
		}

		bigArrayList = theList;
		fileAccessor = new FileAccessor(memoryFilePath);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @return Returns the size of each cache block
	 */
	protected int getTableSize()
	{
		return blockize;
	}

	/**
	 * @return Returns the number of cache blocks
	 */
	protected int getNumberOfCacheBlocks()
	{
		return cacheBlocks;
	}

	/**
	 * Sets the index to add the next element to for the given cache block
	 * 
	 * @param cacheBlockIndex The index for the cache block
	 * @param indexToAdd The next index to add to for the specified cache block
	 */
	private void setCacheTableSpots(int cacheBlockIndex, int indexToAdd)
	{
		cacheTableSpots[cacheBlockIndex] = indexToAdd;
	}

	/**
	 * Sets the mapping for cache blocks that are in memory to the file number on disk
	 * 
	 * @param index The index of the cache block
	 * @param fileNumber The file number
	 */
	private void setCacheTableFiles(int index, int fileNumber)
	{
		cacheTableFiles[index] = fileNumber;
	}

	/**
	 * @param block The cache block
	 * @return Returns if the cache block is full or not
	 */
	protected boolean isCacheFull(int block)
	{
		boolean full = false;

		if(cacheTableSpots[block] >= blockize)
		{
			full = true;
		}

		return full;
	}

	/**
	 * Sets the dirty bit for the given cache block index
	 * 
	 * @param blockIndex Index of the cache block
	 * @param dirty Whether or not the block of cache is dirty
	 */
	protected void setDirtyBit(int blockIndex, boolean dirty)
	{
		dirtyBits[blockIndex] = dirty;
	}

	//this function is called by the add method
	//not considered adding at a spot not at the end
	/**
	 * Called by the add method of BigArrayList
	 * Updates meta data associated with adding an element
	 * 
	 * @param cacheBlockIndex Index of the cache block
	 */
	protected void addEntry(int cacheBlockIndex)
	{
		cacheTableSpots[cacheBlockIndex]++;
		updateUsedList(cacheBlockIndex);
	}

	/**
	 * 
	 * @param elementNumber The element
	 * @return Returns the file number where this element would be
	 */
	protected int getCacheFileNumber(long elementNumber)
	{
		long longTableSize = blockize;
		long longFileNumber = elementNumber / longTableSize;
		Long longObject = new Long(longFileNumber);
		return longObject.intValue();
	}

	/**
	 * Returns the cacheTableFiles spot where the current file/cache block is being held
	 * 
	 * @param fileNumber The file number
	 * @return Returns the cacheTableFiles spot where the current file/cache block is being held
	 */
	protected int getCacheBlockSpot(int fileNumber)
	{
		int blockSpot = -1;

		for(int i=0; i<cacheTableFiles.length && blockSpot == -1; i++)
		{
			if(cacheTableFiles[i] == fileNumber)
			{
				blockSpot = i;
			}
		}

		return blockSpot;
	}

	/**
	 * Returns the spot in cache where this element would be
	 * 
	 * @param elementNumber The element index
	 * @return Returns the spot in cache where this element would be
	 */
	protected int getSpotInCache(long elementNumber)
	{
		long longTableSize = blockize;
		long spotInFile = elementNumber % longTableSize;
		Long longObject = new Long(spotInFile);
		return longObject.intValue();
	}
	
	/**
	 * Returns if the file is in cache or not
	 * 
	 * @param fileNumber The file number index
	 * @return Returns true if the contents of the file are in cache and false otherwise
	 */
	protected boolean isFileInCache(int fileNumber)
	{
		boolean inCache = false;

		for(int i=0; i<cacheTableFiles.length && !inCache; i++)
		{
			if(cacheTableFiles[i] == fileNumber)
			{
				inCache = true;
			}
		}

		return inCache;
	}

	/**
	 * Returns if the element is in cache or not
	 * 
	 * @param elementNumber The element index
	 * @return Returns true if the element number is in cache and false otherwise
	 */
	protected boolean isElementNumberInCache(long elementNumber)
	{
		boolean inCache = false;

		int fileNumber = getCacheFileNumber(elementNumber);
		int blockSpot = getCacheBlockSpot(fileNumber);

		if(isFileInCache(fileNumber))
		{
			long longTableSize = blockize;
			Long longCacheSpot = new Long(elementNumber % longTableSize);
			int cacheSpot = longCacheSpot.intValue();

			if(cacheTableSpots[blockSpot] < cacheSpot)
			{
				inCache = true;
			}
		}

		return inCache;
	}

	/**
	 * Returns the first open location to swap a cache block into or -1 if there are no open spots
	 * 
	 * @return Returns the first open location to swap a cache block into or -1 if there are no open spots
	 */
	protected int getFirstOpenCacheBlock()
	{
		int firstOpen = -1;

		for(int i=0; i<cacheTableFiles.length && firstOpen == -1; i++)
		{
			if(cacheTableFiles[i] == -1)
			{
				firstOpen = i;
			}
		}

		return firstOpen;
	}

	


	//add new to front of list always
		//before doing this, shift down
		//find open starting at index 0

	//block number = fileNumber
	/**
	 * @param blockNumber Block/File number that was just used
	 */
	protected void updateUsedList(int blockNumber)
	{
		int oldPosition = -1;
		int newPosition = mostRecentlyUsedList.length - 1;
		int shiftPosition = 0;

		//find old position if exists
		for(int i=0; i<mostRecentlyUsedList.length; i++)
		{
			if(mostRecentlyUsedList[i] == blockNumber)
			{
				oldPosition = i;
			}

		}

		//set old spot to -1, clear it out
		if(oldPosition != -1)
		{
			mostRecentlyUsedList[oldPosition] = -1;
		}

		//find spot to shift to
		//if open spaces, find first open one
		for(int i=0; i<mostRecentlyUsedList.length; i++)
		{
			if(mostRecentlyUsedList[i] == -1)
			{
				shiftPosition = i;
			}
		}
		
		//shift down
		for(int i=shiftPosition; i<mostRecentlyUsedList.length-1; i++)
		{
			mostRecentlyUsedList[i] = mostRecentlyUsedList[i+1];
		}

		mostRecentlyUsedList[newPosition] = blockNumber;

	}

	/**
	 * @param blockIndex Index to remove from the list
	 */
	private void removeFromUsedList(int blockIndex)
	{
		for(int i=0; i<mostRecentlyUsedList.length; i++)
		{
			if(mostRecentlyUsedList[i] == blockIndex)
			{
				mostRecentlyUsedList[i] = -1;
			}
		}
	}

	/**
	 * Flushes all data in memory to disk
	 */
	protected void flushCache()
	{
		for(int i=0; i<cacheTableFiles.length; i++)
		{
			flushCacheBlock(i);
		}
	}

	/**
	 * Flushed a single cache block to disk
	 * @param blockIndex The index of the cache block
	 */
	private void flushCacheBlock(int blockIndex)
	{
		//write to file
		int fileNumber = cacheTableFiles[blockIndex];

		if(dirtyBits[blockIndex])
		{
			try
			{
				fileAccessor.writeToFile(fileNumber, blockIndex, bigArrayList);
				setDirtyBit(blockIndex, false);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				System.exit(-1);
			}
		}

		//clear list

		bigArrayList.clearList(blockIndex);

		//remove block from used list
		//clear cache for this block
		//clear table for this block

		removeFromUsedList(blockIndex);
		clearCacheBlock(blockIndex);
	}

	/**
	 * Clears a cache block from memory
	 * Only a soft clear
	 * @param blockToClear The block index
	 */
	private void clearCacheBlock(int blockToClear)
	{
		cacheTableSpots[blockToClear] = 0;
		cacheTableFiles[blockToClear] = -1;
	}

	//openCacheBlock = spot where file is in cache
	//blockToFlush = fileNumber in the openCacheBlock spot
	//return the cache spot of the file = openCacheBlock
	/**
	 * Brings the content of the given file number into an available cache block
	 * 
	 * @param fileNumber The file number
	 * @param newBlock Indicated if a new block is being created.  This occurs when adding to the end of the list needs to add to
	 * a cache block that is not in memory because it hasn't been created yet as opposed to being on disk.
	 * @return The index of the spot where the cache block was brought into
	 */
	protected int bringFileIntoCache(int fileNumber, boolean newBlock)
	{
		//clear a spot if there isn't one

		int openCacheBlock = getFirstOpenCacheBlock();

		if(openCacheBlock == -1)
		{
			int blockToFlush = mostRecentlyUsedList[0];
			flushCacheBlock(blockToFlush);
		}

		//read into array list
		//set cacheTableFiles to fileNumber
		//set cacheTableSpots to number of objects read from file
		//update usedList

		openCacheBlock = getFirstOpenCacheBlock();

		//bring into cache without reading from file when new block is being created

		if(!newBlock)
		{
			readFromFile(fileNumber, openCacheBlock);
		}

		setCacheTableFiles(openCacheBlock, fileNumber);
		setCacheTableSpots(openCacheBlock, bigArrayList.getArraySize(openCacheBlock));

		updateUsedList(openCacheBlock);

		return openCacheBlock;
	}

	/*
	public boolean doesFileExist(int fileNumber)
	{
		return customFileAccessor.doesFileExist(fileNumber);
	}

	public void createFile(int fileNumber)
	{
		customFileAccessor.createFile(fileNumber);
	}
	*/

	/**
	 * Reads the data from the given file number / cache block into the specified cache index
	 * 
	 * @param fileNumber The file number / cache block to read in
	 * @param cacheIndex The cache index to populate with the data from the file
	 */
	private void readFromFile(int fileNumber, int cacheIndex)
	{
		try
		{
			fileAccessor.readFromFile(fileNumber, cacheIndex, bigArrayList);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Deletes all data from disk
	 */
	protected void clearMemory()
	{
		fileAccessor.clearMemory();
	}
}