/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.jaer.eventio;

import java.awt.Point;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import eu.seebetter.ini.chips.davis.DavisBaseCamera;
import net.sf.jaer.aemonitor.AEPacketRaw;
import net.sf.jaer.aemonitor.EventRaw.EventType;
import net.sf.jaer.chip.AEChip;
import net.sf.jaer.chip.EventExtractor2D;
import net.sf.jaer.chip.RetinaExtractor;
import net.sf.jaer.event.ApsDvsEvent;
import net.sf.jaer.event.ApsDvsEvent.ReadoutType;
import net.sf.jaer.event.ApsDvsEventPacket;
import net.sf.jaer.event.EventPacket;
import net.sf.jaer.event.OutputEventIterator;

/**
 * This class parses the buffer from the data files or network streams containing jAER 3.0
 * format data, as specified in http://inilabs.com/support/software/fileformat/
 *
 * The most useful public interface for outside is the getJaer2EventBuf(), by this method, the event
 * buffer just include addr and timestamp (like jAER 2.0 did) will be returned. After get this
 * buffer similar to jAER 2.0, all other things will be processed by AEFileInputStream. The stream
 * will be treated like it's a jAER 2.0 buffer.
 *
 * @author min liu
 * @author tobi
 */
public class Jaer3BufferParser {

	private static final Logger log = Logger.getLogger("net.sf.jaer.eventio");
	private int BUFFER_CAPACITY_BYTES = 8 * 1000000;
	private ByteBuffer in = ByteBuffer.allocate(BUFFER_CAPACITY_BYTES);
	// private ByteBuffer out = ByteBuffer.allocate(BUFFER_CAPACITY_BYTES);

	private final int PKT_HEADER_SIZE = 28;

	private int framePixelArrayOffset = 0; // Just for frame events and this is the origin index
	private int translatedArrayIndex = 0; // This is the correct index, in fact it's the transpose of the origin array.

	private PacketDescriptor currentPkt = new PacketDescriptor();
	private long numEvents = 0;

	/**
	 * These points are the first and last pixel APS read out from the array.
	 * Subclasses must set and use these values in the firstFrameAddress and
	 * lastFrameAddress methods and event filters that transform the APS
	 * addresses can modify these values to properly account for the order of
	 * readout, e.g. in RotateFilter.
	 *
	 */
	protected Point apsFirstPixelReadOut = null, apsLastPixelReadOut = null;
	/**
	 * The AEChip object associated with this stream. This field was added for
	 * supported jAER 3.0 format files to support translating bit locations in
	 * events.
	 */
	private AEChip chip = null;

	/**
	 * Field for decoding jaer 3.0 dvs address
	 */
	public static final int JAER3YSHIFT = 2, JAER3YMASK = 32767 << JAER3YSHIFT, // 15 bits from bits 22 to 30
		JAER3XSHIFT = 17, JAER3XMASK = 32767 << JAER3XSHIFT, // 15 bits from bits 12 to 21
		JAER3POLSHIFT = 1, JAER3POLMASK = 1 << JAER3POLSHIFT; // , // 1 bit at bit 11
	/**
	 * Field for decoding jaer 3.0 aps address
	 */
	public static final int JAER3APSYSHIFT = 0, JAER3APSYMASK = 65535 << JAER3APSYSHIFT, // 16 bits from bits 16 to 31
		JAER3APSXSHIFT = 16, JAER3APSXMASK = 65535 << JAER3APSXSHIFT; // 16 bits from bits 0 to 15

	private boolean inFrameEvent = false;
	private int frameCurrentEventOffset;
	private boolean readOutType = false;
        
        private static AEChip ORIGINAL_CHIP = null;
	private static EventExtractor2D ORIGINAL_EVENT_EXTRACTOR = null;

	public class PacketHeader {

		EventType eventType = EventType.SpecialEvent;
		int eventSource = 0;
		int eventSize = 0;
		int eventTSOffset = 0;
		int eventTSOverflow = 0;
		int eventCapacity = 0;
		int eventNumber = 0;
		int eventValid = 0;
	}

	public class PacketDescriptor {

		private PacketHeader pktHeader;
		private int pktPosition;

		public PacketDescriptor() {
			pktHeader = new PacketHeader();
			pktPosition = 0;
		}

		public PacketHeader GetPktHeader() {
			return pktHeader;
		}

		public int GetPktPosition() {
			return pktPosition;
		}

		public void SetPktHeader(PacketHeader d) {
			pktHeader = d;
		}

		public void SetPosition(int position) {
			pktPosition = position;
		}
	}

	public class FrameDescriptor {
		int frameInfo; // see AEDAT3.0 spec for frame event
		int startOfCaptureTimestamp;
		int endOfCaptureTimestamp;
		int startOfExposureTimestamp;
		int endOfExposureTimestamp;
		int xLength; // width
		int yLength; // height
		int xPosition; // offset of LL corner in pixels
		int yPosition; // offset of LL corner in pixels
	}

	// private EventRaw tmpEventRaw = new EventRaw();

	/**
	 * This method returns is used to find the packet header.
	 *
	 * @param startPosition:
	 *            The position
	 * @param direction:
	 *            The search direction, 1 is forward and 0 is backward.
	 * @return: The packet header which the startPosition nearest to according to the search direction.
	 * @throws IOException
	 */
	private PacketDescriptor searchPacketHeader(int startPosition, int direction) throws IOException {
		PacketDescriptor pkt = new PacketDescriptor();
		PacketHeader d = pkt.GetPktHeader();
		int position = pkt.GetPktPosition();

		int eventTypeInt;
		int currentPosition = in.position(); // store current position, guarntee the position didn't change in this
												// function

		if ((direction != 1) && (direction != -1)) {
			log.warning("Search direction can only be 1(forward) or -1(backward!)");
			return null;
		}

		in.position(startPosition);

		while ((in.position() <= (in.limit() - PKT_HEADER_SIZE)) && (in.position() >= 0)) {
			int currentSearchPosition = in.position();
			eventTypeInt = in.getShort();
			// By default, eventTypeInt should range from 0 to 7
			if ((eventTypeInt > 7) || (eventTypeInt < 0)) {
				in.position(currentSearchPosition + direction);
				continue;
			}

			d.eventType = EventType.values()[eventTypeInt];
			d.eventSource = in.getShort();
			d.eventSize = in.getInt();
			if (d.eventSize <= 0) {
				in.position(currentSearchPosition + direction);
				continue;
			}
			d.eventTSOffset = in.getInt(); // eventoverflow

			// timestamp offset can only be 6(Configuration Event), 12 (Frame Event) or 4(other events).
			if (d.eventType == EventType.ConfigEvent) {
				if (d.eventTSOffset != 6) {
					in.position(currentSearchPosition + direction);
					continue;
				}
			}
			else if (d.eventType == EventType.FrameEvent) {
				if (d.eventTSOffset != 12) {
					in.position(currentSearchPosition + direction);
					continue;
				}
			}
			else {
				if (d.eventTSOffset != 4) {
					in.position(currentSearchPosition + direction);
					continue;
				}
			}
			d.eventTSOverflow = in.getInt(); // eventTSOverflow
			d.eventCapacity = in.getInt(); // eventcapacity

			d.eventNumber = in.getInt(); // eventnumber
			if (d.eventNumber <= 0) {
				in.position(currentSearchPosition + direction);
				continue;
			}
			// if(d.eventNumber != d.eventCapacity) continue;

			d.eventValid = in.getInt(); // eventValid
			if (d.eventValid < 0) {
				in.position(currentSearchPosition + direction);
				continue;
			}

			position = in.position() - PKT_HEADER_SIZE;

			if (d.eventNumber >= d.eventValid) {
				break;
			}

		}

		if (in.position() > (in.limit() - PKT_HEADER_SIZE)) {
			in.position(currentPosition); // restore the position, because the byteBuffer in AEFileinputStream share the
											// position with in.
			return null;
		}

		pkt.SetPktHeader(d);
		pkt.SetPosition(position);

		in.position(currentPosition); // restore the position, because the byteBuffer in AEFileinputStream share the
										// position with in.
		return pkt;
	} // searchPacketHeader

	/**
	 * The method is to find the current packet header which the target position belongs to.
	 *
	 * @param targetPosition:
	 *            the target position
	 * @return current packet header.
	 * @throws IOException
	 */
	private PacketDescriptor getCurrentPkt(int targetPosition) throws IOException {

		PacketDescriptor pkt = searchPacketHeader(targetPosition, -1);

		if ((targetPosition - pkt.pktPosition) > (((pkt.pktHeader.eventSize) * (pkt.pktHeader.eventNumber)) + PKT_HEADER_SIZE)) {
			log.warning("Current position data is an invalid data, it doesn't belong to any packet!");
			return null;
		}
		else {
			return pkt;
		}
	} // getCurrentPkt

	/**
	 * Get the next packet header of the target position
	 *
	 * @param targetPosition:
	 *            target position, which means the position you want to use
	 * @return the next packet header of the target position.
	 * @throws IOException
	 */
	private PacketDescriptor getNextPkt(int targetPosition) throws IOException {
		return searchPacketHeader(targetPosition, 1);
	}
        
        /**
	 * Constructor of the jaer3BufferParser.
	 *
	 * @param byteBuffer
	 * @param chip
	 * @throws IOException
	 */
	public Jaer3BufferParser(ByteBuffer byteBuffer, AEChip chip) throws IOException {
		in = byteBuffer;
		in.order(ByteOrder.LITTLE_ENDIAN); // AER3.0 spec is little endian
		this.chip = chip;

                /* Here is the logic:
                 * The chip and extractor will be updated unless the chip changed such as by the user.
                 * It makes the chip and the extractor are alwayse associated with each other. 
                */
                if(this.chip != ORIGINAL_CHIP) {
                    ORIGINAL_CHIP = this.chip;
                    ORIGINAL_EVENT_EXTRACTOR = this.chip.getEventExtractor();
                }
		chip.setEventExtractor(new jaer3EventExtractor(chip));

		currentPkt = searchPacketHeader(0, 1);

		try {
			numEvents = bufferNumEvents();
		}
		catch (IOException ex) {
			log.warning(ex.toString());
			Logger.getLogger(AEFileInputStream.class.getName()).log(Level.SEVERE, null, ex);
		}
	} // Jaer3BufferParser       

	/**
	 * Constructor of the jaer3BufferParser
	 *
	 * @param byteBuffer
	 * @param chip
	 * @throws IOException
	 */
	public Jaer3BufferParser(MappedByteBuffer byteBuffer, AEChip chip) throws IOException {
            this((ByteBuffer) byteBuffer, chip);
	} // Jaer3BufferParser
        
        /**
	 * gets the size of the stream in events
	 *
	 * @return size in events
	 */
	public long size() {
		return numEvents;
	}

	public int getCurrentEventOffset() throws IOException {
		int currentPosition = in.position();
		int nextPktPos = 0;
		try {
			nextPktPos = currentPkt.pktPosition + (currentPkt.pktHeader.eventNumber * currentPkt.pktHeader.eventSize) + PKT_HEADER_SIZE;
		}
		catch (NullPointerException npe) {
			currentPkt = getCurrentPkt(currentPosition);
			if (currentPkt == null) {
				return -1; // It's an invalid position, it doesn't have packet header and event data.
			}
			nextPktPos = currentPkt.pktPosition + (currentPkt.pktHeader.eventNumber * currentPkt.pktHeader.eventSize) + PKT_HEADER_SIZE;
		}

		// current position is not in the current packet, so we need to find the packet the current position belongs to
		if ((currentPosition >= nextPktPos) || (currentPosition <= currentPkt.pktPosition)) {
			currentPkt = getCurrentPkt(currentPosition);
			if (currentPkt == null) {
				return -1; // It's an invalid position, it doesn't have packet header and event data.
			}
		}

		int currentPktPos = currentPkt.pktPosition;
		PacketHeader packetHeader = currentPkt.pktHeader;
		// int nextPktPos = currentPktPos + packetHeader.eventNumber * packetHeader.eventSize + pkt.PKT_HEADER_SIZE;

		// Now we get the correct currentPktPos
		if (((currentPosition - currentPktPos) <= PKT_HEADER_SIZE) && ((currentPosition - currentPktPos) >= 0)) { 													// offset
			return currentPktPos + PKT_HEADER_SIZE;
		}

		// currentPktPos + PKT_HEADER_SIZE is the first event offset, so (currentPosition - (currentPktPos +
		// PKT_HEADER_SIZE))%eventSize is the distance
		// between current position and current event offset
		int eventSize = packetHeader.eventSize;
		return currentPosition - ((currentPosition - (currentPktPos + PKT_HEADER_SIZE)) % eventSize);
	}

	/**
	 * Get the next event offset of the current position
	 *
	 * @return the next event offset of the current position
	 * @throws IOException
	 */
	public int getNextEventOffset() throws IOException {
		int currentEventOffset = getCurrentEventOffset();
		int currentPosition = in.position();

		if (-1 == currentEventOffset) { // Current position is an invalid data or in the file end,
			currentPkt = getNextPkt(currentPosition);
			if (null == currentPkt) { // It's in the file end
				return -1;
			}
			else {
				return currentPkt.pktPosition + PKT_HEADER_SIZE; // It's an invalid data, so we need to use the next
																	// packet
			}
		}

		int currentPktPos = currentPkt.pktPosition;
		PacketHeader packetHeader = currentPkt.pktHeader;
		int eventSize = packetHeader.eventSize;
		int nextPktPos = currentPktPos + (packetHeader.eventNumber * packetHeader.eventSize) + PKT_HEADER_SIZE;

		// It means it's on the event offset right now, so the next event of this position is itself
		// We can return the currentEventOffset directly, no matter it is in the middle events or the last event
		if (currentPosition == currentEventOffset) {
			return currentEventOffset;
		}

		// The current position is in the last event of the current packet and not the event header, so the next event
		// will be in the next packet.
		// We should update the currentPktPos first.
		if ((currentPosition + eventSize) >= nextPktPos) {
			currentPkt = getNextPkt(nextPktPos);
			if (null == currentPkt) { // It's in the file end
				return -1;
			}
			else {
				return currentPkt.pktPosition + PKT_HEADER_SIZE; // Use the next packet
			}
		}

		// Now we get the correct currentPktPos and the current position is in the header.
		if (((currentPosition - currentPktPos) <= PKT_HEADER_SIZE) && ((currentPosition - currentPktPos) >= 0)) { 														// offset
			return currentPktPos + PKT_HEADER_SIZE;
		}

		return currentEventOffset + eventSize;
	} // getNextEventOffset

	/**
	 * This function is used to get the last time stamp of the buffer
	 *
	 * @return the last time stamp of the buffer.
	 * @throws IOException
	 */
	public int getLastTimeStamp() throws IOException {
		int currentPosition = in.position();
		PacketDescriptor pkt = getCurrentPkt(in.limit() - PKT_HEADER_SIZE);
		int lastTs;
		if (pkt.pktHeader.eventNumber == pkt.pktHeader.eventValid) {
			int position = pkt.pktPosition + PKT_HEADER_SIZE + ((pkt.pktHeader.eventNumber - 1) * pkt.pktHeader.eventSize)
				+ pkt.pktHeader.eventTSOffset;
			in.position(position);
			lastTs = in.getInt();
		}
		else {
			ByteBuffer tmpBuffer = ByteBuffer.allocate(16);
			in.position(pkt.pktPosition);
			for (int i = 0; i < pkt.pktHeader.eventValid; i++) {
				tmpBuffer = getJaer2EventBuf(); // TODO, catch BufferUnderFlowException() here
			}
			tmpBuffer.getInt(); // event type
			tmpBuffer.getInt(); // addr
			lastTs = tmpBuffer.getInt();
		}

		in.position(currentPosition); // Restore last position
		return lastTs;
	} // getLastTimeStamp

	/**
	 * This function is used to find the next valid event offset, because not all the events are valid, we should skip
	 * the invalid events.
	 *
	 * @return the next valid event offset.
	 * @throws IOException
	 */
	public int getNextValidEventOffset() throws IOException {
		int nextEventOffset = getNextEventOffset();

		if (-1 == nextEventOffset) {
			log.warning("Reach the end of the buffer, can't read data!");
			throw new BufferUnderflowException();
		}

		final int validMask = 0x00000001;
		int eventFirstInt;
		in.position(nextEventOffset);
		eventFirstInt = in.getInt();

		// This while loop is used to exclude the invalid events
		while (((eventFirstInt & validMask) != 1) || (currentPkt.pktHeader.eventType == EventType.Imu6Event)) {
			nextEventOffset = getNextEventOffset();
			if (-1 == nextEventOffset) {
				log.warning("Reach the end of the buffer, can't read data!");
				throw new BufferUnderflowException();
			}
			in.position(nextEventOffset);
			eventFirstInt = in.getInt();
		}
		return nextEventOffset;
	} // getNextValidEventOffset

	/**
	 * Set the inFrameEvent, inFrameEvent is a flag used to represent the current event is in the frame or not.
	 * The reason why we use this flag is that a whole frame events is divided into several pixels data, and every
	 * pixel data is faked as one single event.
	 *
	 * @param frameEventFlg:
	 *            true: current event is in a frame, false: current event is not in a frame
	 */
	public void setInFrameEvent(boolean frameEventFlg) {
		this.inFrameEvent = frameEventFlg;
	}

	/**
	 * This function is a very important function. It returns the 16-byte events (eventtype, addr, ts and pixeldata)
	 * like it's a jaer2 event.
	 * Pixeldata is only used by frame event, in other case it's 0.
	 *
	 * @return one buffer that contains the standard 16-byte event.
	 * @throws IOException
	 */
	public ByteBuffer getJaer2EventBuf() throws IOException {
		ByteBuffer jaer2Buffer = ByteBuffer.allocate(16);

		int nextEventOffset = 0;
		if (!inFrameEvent) {
			nextEventOffset = getNextValidEventOffset();
			frameCurrentEventOffset = nextEventOffset;
			if (currentPkt.pktHeader.eventType == EventType.FrameEvent) {
				int xlengthOffset = frameCurrentEventOffset + 20;
				int xlength = in.getInt(xlengthOffset);
				int ylengthOffset = frameCurrentEventOffset + 24;
				int ylength = in.getInt(ylengthOffset);
				translatedArrayIndex = (2 * xlength * ylength) - 1;
			}
		}

		// First check if it's a frame event, if it's a frame event, we must check
		// whether the current event is finished or not
		if (currentPkt.pktHeader.eventType == EventType.FrameEvent) {
			int xlengthOffset = frameCurrentEventOffset + 20;
			int xlength = in.getInt(xlengthOffset);
			int ylengthOffset = frameCurrentEventOffset + 24;
			int ylength = in.getInt(ylengthOffset);
			// int channelNumber = (in.getInt(frameCurrentEventOffset) & 0xe) >> 1;
			int tsOffset = currentPkt.pktHeader.eventTSOffset;
			int ts = in.getInt(frameCurrentEventOffset + tsOffset);

			if (translatedArrayIndex >= 0) {
				inFrameEvent = true;
				// framePixelArrayOffset = translatedArrayIndex;
				jaer2Buffer.putInt(currentPkt.pktHeader.eventType.getValue()); // type
				// jaer2Buffer.putInt(((framePixelArrayOffset/ylength)<< 16) + framePixelArrayOffset%ylength); //addr

				int jaer2FrameAddr;
				int data;

				if ((translatedArrayIndex >= (xlength * ylength)) && (translatedArrayIndex <= ((2 * xlength * ylength) - 1))) {
					jaer2FrameAddr = ((((translatedArrayIndex - (xlength * ylength)) / ylength)) << 17)
						+ (((translatedArrayIndex - (xlength * ylength)) % ylength) << 2) + 0;

					// Reset Read Array
					framePixelArrayOffset = (239 - ((translatedArrayIndex - (xlength * ylength)) / ylength))
						+ (xlength * ((translatedArrayIndex - (xlength * ylength)) % ylength));

					int dataOffset = 36 + (2 * (framePixelArrayOffset));
					if ((frameCurrentEventOffset + dataOffset) >= in.limit()) {
						throw new BufferUnderflowException(); // Reach the end of the buffer
					}
					data = in.getShort(frameCurrentEventOffset + dataOffset);

				}
				else {
					jaer2FrameAddr = ((((translatedArrayIndex) / ylength)) << 17) + (((translatedArrayIndex) % ylength) << 2) + 1;

					// Signal Read Array
					data = 0;
				}

				jaer2Buffer.putInt(jaer2FrameAddr);
				jaer2Buffer.putInt(ts); // ts
				jaer2Buffer.putInt(data); // pixeldata
				jaer2Buffer.flip();

				// outFile.writeInt(jaer2FrameData);
				// outFile.writeInt(ts);
				// outFile.close();
				translatedArrayIndex -= 1;
				return jaer2Buffer;
			}
			else {
				inFrameEvent = false;
				readOutType = !readOutType;
				// framePixelArrayOffset = 0;
				in.position(frameCurrentEventOffset + currentPkt.pktHeader.eventSize);
				return getJaer2EventBuf();
			}

		}

		int dataOffset = GetDataOffset(currentPkt.pktHeader);
		int tsOffset = currentPkt.pktHeader.eventTSOffset;

		in.position(nextEventOffset + dataOffset);
		int addr = in.getInt();
		in.position(nextEventOffset + tsOffset);
		int ts = in.getInt();

		jaer2Buffer.putInt(currentPkt.pktHeader.eventType.getValue());
		jaer2Buffer.putInt(addr);
		jaer2Buffer.putInt(ts);
		jaer2Buffer.putInt(0); // pixelData just for frame event, other events don't use it;
		jaer2Buffer.flip();
		return jaer2Buffer;
	} // getJaer2EventBuf

	/**
	 * This function gets the total events number of the buffer
	 *
	 * @return the total events number of the buffer
	 * @throws IOException
	 */
	public long bufferNumEvents() throws IOException {
		PacketDescriptor pkt = getNextPkt(0);
		long numEvents = 0;
		while (pkt != null) {
			if (pkt.pktHeader.eventType == EventType.PolarityEvent) {
				numEvents += pkt.pktHeader.eventValid;
			}
			if (pkt.pktHeader.eventType == EventType.FrameEvent) {
				int xlength = in.getInt(pkt.pktPosition + PKT_HEADER_SIZE + 20);
				int ylength = in.getInt(pkt.pktPosition + PKT_HEADER_SIZE + 24);

				numEvents += 2* (xlength * ylength * (pkt.pktHeader.eventValid)); // One array has been divided into 2 arrays, so the event numbers in Frame should also increase
			}
			try {
				pkt = getNextPkt(pkt.pktPosition + ((pkt.pktHeader.eventNumber) * (pkt.pktHeader.eventSize)));
			}
			catch (IllegalArgumentException e) { // It means it reaches the buffer end
				// log.warning("Reaches the end of the buffer!");
				pkt = null;
			}
		}
		return numEvents;
	}

	/**
	 * Returns the data offset of different events.
	 *
	 * @param packetHeader
	 *            the current packet header.
	 * @return Returns the data offset of different events.
	 * @throws IOException
	 */
	private int GetDataOffset(PacketHeader packetHeader) throws IOException {
		int eventDataOffset = 0;

		switch (packetHeader.eventType) {
			case PolarityEvent:
				eventDataOffset = 0;
				break;
			case FrameEvent:
				eventDataOffset = 36 + (2 * framePixelArrayOffset); // TODO, Consider the channel number
				break;
			case SampleEvent:
				eventDataOffset = 0;
				break;
			case ConfigEvent:
				eventDataOffset = 0;
				break;
			case Imu6Event:
				eventDataOffset = 0;
				break;
			case Imu9Event:
				eventDataOffset = 0;
				break;
			default:
				eventDataOffset = 0;
		}

		return eventDataOffset;
	}

	/**
	 * Gets the current packet header position
	 *
	 * @return the current packet header position
	 */
	public int getCurrentPktPos() {
		return currentPkt.pktPosition;
	}

	/**
	 * Get the current packet
	 *
	 * @return the current packet header
	 */
	public PacketHeader getPacketHeader() {
		return currentPkt.pktHeader;
	}

	/**
	 * Sets the parser's buffer
	 *
	 * @param BufferToBeProcessed
	 *            the parser to be processed
	 */
	public void setInBuffer(ByteBuffer BufferToBeProcessed) {
		in = BufferToBeProcessed; // To change body of generated methods, choose Tools | Templates.
	}

	/**
	 * Sets the buffer's order
	 *
	 * @param order
	 *            two orders, Big endian and little endian.
	 */
	public void setInBufferOrder(ByteOrder order) {
		in.order(order);
	}

	/**
	 * This extractor is an uniform extractor for all the jaer 3.0 data. It only works for 3.0 and not cannot be used on
	 * 2.0.
	 */
	public class jaer3EventExtractor extends RetinaExtractor {

		protected int autoshotEventsSinceLastShot = 0; // autoshot counter

		public jaer3EventExtractor(final AEChip chip) {
			super(chip);
		}

		/**
		 * extracts the meaning of the raw events.
		 *
		 * @param in
		 *            the raw events, can be null
		 * @return out the processed events. these are partially processed
		 *         in-place. empty packet is returned if null is supplied as in.
		 */
		@Override
		synchronized public EventPacket extractPacket(final AEPacketRaw in) {
			if (out == null) {
				out = new ApsDvsEventPacket(ApsDvsEvent.class); // In order to be general, we make the packet's event
																// ApsDvsEvent.
			}
			else {
				out.clear();
			}
			out.setRawPacket(in);
			if (in == null) {
				return out;
			}
			final int n = in.getNumEvents(); // addresses.length;
			final int sx1 = getChip().getSizeX() - 1;
			getChip().getSizeY();

			final int[] addrs = in.getAddresses();
			final int[] timestamps = in.getTimestamps();
			final EventType[] etypes = in.getEventtypes();
			final int[] pixelDatas = in.getPixelDataArray();
			final OutputEventIterator outItr = out.outputIterator();

			// NOTE we must make sure we write ApsDvsEvents when we want them, not reuse the IMUSamples

			// at this point the raw data from the USB IN packet has already been digested to extract timestamps,
			// including timestamp wrap events and timestamp resets.
			// The datas array holds the data, which consists of a mixture of AEs and ADC values.
			// Here we extract the datas and leave the timestamps alone.
			// System.out.println("Extracting new packet "+out);
			for (int i = 0; i < n; i++) {
				// events and still delivering frames
				final int addr = addrs[i];
				final int data = pixelDatas[i];
                                
                                if(etypes[i] == null) { // The packet is not from AEDAT3.0 file or network, it comes from AEDAT2.0, we need restore it.
                                    chip.setEventExtractor(ORIGINAL_EVENT_EXTRACTOR); // Restore the default extractor
                                    return chip.getEventExtractor().extractPacket(in);         
                                }

                            
                                switch (etypes[i]) {
                                      case PolarityEvent:
                                              readDVS(outItr, addr, timestamps[i]);
                                              break;
                                      case FrameEvent:
                                              readFrame(outItr, addr, data, timestamps[i]);
                                              break;
                                      case SampleEvent:
                                              // readSample();
                                              break;
                                      case ConfigEvent:
                                              // readConfig();
                                              break;
                                      case Imu6Event:
                                              // readImu6();
                                              break;
                                      case Imu9Event:
                                              // readImu9();;
                                              break;
                                      default:
                                }  
                                
				 
				/*
				 * if ((getAutoshotThresholdEvents() > 0) && (autoshotEventsSinceLastShot >
				 * getAutoshotThresholdEvents())) {
				 * takeSnapshot();
				 * autoshotEventsSinceLastShot = 0;
				 * }
				 */
			}
			return out;
		}// extractPacket

		/**
		 * Gets the next ApsDvsEvent in the stream
		 *
		 * @param outItr
		 *            the iterator of the output stream.
		 * @return
		 */
		protected ApsDvsEvent nextApsDvsEvent(final OutputEventIterator outItr) {
			final ApsDvsEvent e = (ApsDvsEvent) outItr.nextOutput();
			e.reset();
			return e;
		}

		/**
		 * Extractor the DVS events.
		 *
		 * @param outItr
		 *            the iterator of the output stream
		 * @param data
		 *            data, for DVS, it's the address
		 * @param timestamp
		 *            timestamp of the event
		 */
		protected void readDVS(final OutputEventIterator outItr, final int data, final int timestamp) {
			final int sx1 = getChip().getSizeX() - 1;
			final ApsDvsEvent e = nextApsDvsEvent(outItr);

			e.address = data;
			e.timestamp = timestamp;
			e.polarity = ((data & JAER3POLMASK) >> JAER3POLSHIFT) == (JAER3POLMASK >> JAER3POLSHIFT) ? ApsDvsEvent.Polarity.On
				: ApsDvsEvent.Polarity.Off;
			e.type = 0;
			e.x = (short) (sx1 - ((data & JAER3XMASK) >>> JAER3XSHIFT));
			e.y = (short) ((data & JAER3YMASK) >>> JAER3YSHIFT);

			e.setReadoutType(ReadoutType.DVS);

			// autoshot triggering
			autoshotEventsSinceLastShot++; // number DVS events captured here
		}

		/**
		 * Extractor the APS Events.
		 *
		 * @param outItr
		 *            the iterator of the output stream
		 * @param addr
		 *            address of the event
		 * @param data
		 *            pixel data of the frame event
		 * @param timestamp
		 *            time stamp of the event
		 */
		protected void readFrame(final OutputEventIterator outItr, final int addr, final int data, final int timestamp) {
			final int sx1 = getChip().getSizeX() - 1;
			final ApsDvsEvent e = nextApsDvsEvent(outItr);
			// APS event
			// We first calculate the positions, so we can put events such as StartOfFrame at their
			// right place, before the actual APS event denoting (0, 0) for example.

			final short x = (short) (((addr & JAER3XMASK) >> JAER3XSHIFT));
			final short y = (short) ((addr & JAER3YMASK) >> JAER3YSHIFT);
			// final short x = (short) (((addr & (1023 << 12)) >>> 12));
			// final short y = (short) ((addr & (511 << 22)) >>> 22);

			final boolean pixFirst = firstFrameAddress(x, y); // First event of frame (addresses get flipped)
			final boolean pixLast = lastFrameAddress(x, y); // Last event of frame (addresses get flipped)

			ApsDvsEvent.ReadoutType readoutType = ApsDvsEvent.ReadoutType.Null;
			switch (addr & 0x3) {
				case 0:
					readoutType = ApsDvsEvent.ReadoutType.ResetRead;
					break;

				case 1:
					readoutType = ApsDvsEvent.ReadoutType.SignalRead;
					break;

				case 3:
					log.warning("Event with readout cycle null was sent out!");
					break;

				default:

					break;
			}

			e.setAdcSample(data);
			e.setReadoutType(readoutType);
			e.timestamp = timestamp;
			e.address = addr;
			e.type = 2;
			e.x = x;
			e.y = y;

			if (pixLast && (readoutType == ApsDvsEvent.ReadoutType.SignalRead)) {
				createApsFlagEvent(outItr, ApsDvsEvent.ReadoutType.EOF, timestamp);

				if (Jaer3BufferParser.this.chip instanceof DavisBaseCamera) {
					DavisBaseCamera davisBasechip = (DavisBaseCamera) (Jaer3BufferParser.this.chip);
					davisBasechip.getFrameCount();
					DavisBaseCamera.DavisEventExtractor extractor = davisBasechip.new DavisEventExtractor(davisBasechip);
					extractor.increaseFrameCount(1);
				}

			}

			if (Jaer3BufferParser.this.chip instanceof DavisBaseCamera) {
				DavisBaseCamera davisBasechip = (DavisBaseCamera) (Jaer3BufferParser.this.chip);
				if ((davisBasechip.getAutoshotThresholdEvents() > 0)
					&& (autoshotEventsSinceLastShot > davisBasechip.getAutoshotThresholdEvents())) {
					davisBasechip.takeSnapshot();
					autoshotEventsSinceLastShot = 0;
				}
			}
		}

		protected void setFrameCount(final int i) {
			// frameCount = i;
		}

		/**
		 * Subclasses should set the apsFirstPixelReadOut and apsLastPixelReadOut
		 *
		 * @param x
		 *            the x location of APS readout
		 * @param y
		 *            the y location of APS readout
		 * @see #apsFirstPixelReadOut
		 */
		public boolean firstFrameAddress(final short x, final short y) {
			final boolean yes = (x == -1) && (y == -1);
			return yes;
		}

		/**
		 * Subclasses should set the apsFirstPixelReadOut and apsLastPixelReadOut
		 *
		 * @param x
		 *            the x location of APS readout
		 * @param y
		 *            the y location of APS readout
		 * @see #apsLastPixelReadOut
		 */
		public boolean lastFrameAddress(final short x, final short y) {
			final boolean yes = (x == 0) && (y == 0);
			return yes;
		}

		/**
		 * creates a special ApsDvsEvent in output packet just for flagging APS
		 * frame markers such as start of frame, reset, end of frame.
		 *
		 * @param outItr
		 * @param flag
		 * @param timestamp
		 * @return
		 */
		protected ApsDvsEvent createApsFlagEvent(final OutputEventIterator outItr, final ApsDvsEvent.ReadoutType flag,
			final int timestamp) {
			final ApsDvsEvent a = nextApsDvsEvent(outItr);
			a.timestamp = timestamp;
			a.setReadoutType(flag);
			return a;
		}
	}
}
