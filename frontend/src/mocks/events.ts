import { http, HttpResponse } from "msw";
import { apiHeaders, problem } from "./problem";

// Mock of E-07 with the DOC-37 example event ("Hòa nhạc Giao Mùa"); any other id is 404.

export const MOCK_EVENT_ID = "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35";

const event = {
  eventId: MOCK_EVENT_ID,
  name: "Hòa nhạc Giao Mùa",
  description: "Đêm nhạc giao mùa với dàn nhạc giao hưởng.\nCửa mở lúc 18:30.",
  venue: "Nhà hát Hòa Bình, TP.HCM",
  timezone: "Asia/Ho_Chi_Minh",
  imageUrl: "/media/0199b1c2-1111-7aaa-8bbb-ccccddddeeee",
  startsAt: "2026-11-12T12:00:00Z",
  endsAt: "2026-11-12T15:00:00Z",
  saleStartsAt: "2026-11-05T03:00:00Z",
  saleEndsAt: "2026-11-12T10:00:00Z",
  displayStatus: "ON_SALE",
  highDemand: true,
  holdMinutes: 10,
  hasSeatMap: true,
  mapVersion: 2,
  organizer: { name: "Giao Mùa Studio", contactEmail: "lienhe@giaomua.example" },
  ticketTypes: [
    {
      ticketTypeId: "0199b1c2-7b02-7f10-8a22-3c4d5e6f7082",
      name: "Hạng A",
      model: "SEAT",
      price: 1200000,
      currency: "VND",
      colorIndex: 1,
      soldOut: false,
    },
    {
      ticketTypeId: "0199b1c2-7b03-7f10-8a22-3c4d5e6f7083",
      name: "Fanzone",
      model: "ZONE",
      price: 450000,
      currency: "VND",
      colorIndex: 2,
      soldOut: false,
    },
  ],
};

export const eventHandlers = [
  http.get("*/api/v1/events/:eventId", ({ params }) =>
    params.eventId === MOCK_EVENT_ID
      ? HttpResponse.json(event, { headers: apiHeaders({ "Cache-Control": "public, max-age=5" }) })
      : problem(404, "NOT_FOUND"),
  ),
];
