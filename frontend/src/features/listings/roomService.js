import axiosInstance from "../../api/axiosInstance";

const ROOM_URL = "/api/v4/rooms";

// POST /{listingId}/rooms   body = AddRoomDto { roomType, price, totalRooms, availableRooms }
export const createRoom = (listingId, room) => {
  return axiosInstance.post(`${ROOM_URL}/${listingId}/rooms`, room);
};

// PUT /{listingId}/rooms/{roomId}
export const updateRoom = (listingId, roomId, room) => {
  return axiosInstance.put(`${ROOM_URL}/${listingId}/rooms/${roomId}`, room);
};

// DELETE /{listingId}/rooms/{roomId}
// If this removes the last room of a published listing, the server moves the listing back to draft.
export const deleteRoom = (listingId, roomId) => {
  return axiosInstance.delete(`${ROOM_URL}/${listingId}/rooms/${roomId}`);
};
