package nio.multicast.implementation;

enum MESSAGE_TYPE{
	MESSAGE, //+clock +pidS +message
	ACK, //+clock +pidM +pidS
	ADD_MEMBER,
	NOT_RECEIVED_YET, 
	ID
}
