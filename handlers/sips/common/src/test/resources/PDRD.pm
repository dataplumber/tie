package MTVS::Export::PDRD;

use strict;

#-----------------------------------------------------------------------------

=head1 NAME

MTVS::Export::PDRD - Product Delivery Record Discrepancy (PDRD) class.

=head1 SYNOPSIS

use MTVS::Export::PDRD;

$pdrd = new MTVS::Export::PDRD( $PDRD_name );

$message_type = $pdrd->get_message_type;

$disposition  = $pdrd->get_disposition;

$no_file_grps = $pdrd->get_no_file_grps;

@groups       = $pdrd->get_groups;

=head1 DESCRIPTION

The PDRD class provides methods for accessing the information in the PDRD
object created by the new method.  The new method reads the PDRD given as
the input parameter and assigns its values to a data structure.  The data
structure will differ depending on whether the PDRD is a SHORTPDRD or
LONGPDRD.

=begin text

    For SHORTPDRD:

    $pdrd->{MESSAGE_TYPE}           - type of PDRD (SHORTPDRD).
    $pdrd->{DISPOSITION}            - disposition of ingest request.

    For LONGPDRD:

    $pdrd->{MESSAGE_TYPE}           - type of PDRD (LONGPDRD).
    $pdrd->{NO_FILE_GRPS}           - number of file groups in the PDRD.
    $pdrd->{GROUPS}[i]{DATA_TYPE}   - ECS data type.
    $pdrd->{GROUPS}[i]{DISPOSITION} - disposition of ingest request for file 
                                      group.

=end text

where i is between 0 and $pdrd->{NO_FILE_GRPS}-1.

The data structure is given for reference.  All accesses to the data structure  
should be made through the get_message_type, get_disposition, 
get_no_file_grps, and get_groups methods.  The get_disposition method will 
fail if the MESSAGE_TYPE is LONGPDRD.  The get_no_file_grps and get_groups 
methods will fail if the MESSAGE_TYPE is SHORTPDRD.

=over 4

=item $pdrd = new MTVS::Export::PDRD( $PDRD_name )

Constructs a PDRD object and initializes it with the contents of the PDRD file.
$PDRD_name = full path name of a PDRD file.  Returns reference to a PDRD 
object if the PDRD file is opened successfully, otherwise 0 is returned.

=back

=cut

#-----------------------------------------------------------------------------
sub new {

    # Construct object
    my $class = shift;
    my $self = { };
    bless( $self, $class );

    # Initialize object

    # Get name of PDRD
    my $name = shift;
    my $junk;

    # Open PDRD
    open( PDRD, $name ) or return 0;

    # Parse PDRD
    while ( <PDRD> ) {

        # remove semicolon
        ($_, $junk) = split ';';
        $_ =~ s/^\s+//;   # Remove leading blanks
        $_ =~ s/\s+$//;   # Remove trailing blanks

        # Extract parameter and value from line
        my ($parameter,$value) = /(\w+)\s*=\s*"?([^"]+)/x;

	# Assign parameter and value to object hash reference
	if ( $parameter eq "DATA_TYPE" and 
	     $self->{MESSAGE_TYPE} eq "LONGPDRD" ) 
	{
	    push @{ $self->{GROUPS} }, { $parameter => $value };
	} elsif ( $parameter eq "DISPOSITION" and 
	          $self->{MESSAGE_TYPE} eq "LONGPDRD" ) 
	{
	    $self->{GROUPS}[$#{ $self->{GROUPS} }]{$parameter}= $value;
	} else {
	    $self->{$parameter} = $value;
	} #endif

    } #endwhile

    # Return object
    return $self;

} #endsub

#-----------------------------------------------------------------------------

=over 4

=item $message_type = $pdrd->get_message_type()

Returns $self->{MESSAGE_TYPE}, the MESSAGE_TYPE of the PDRD.

=back

=cut

#-----------------------------------------------------------------------------
sub get_message_type {
    my $self = shift;
    return $self->{MESSAGE_TYPE};
}

#-----------------------------------------------------------------------------

=over 4

=item $pdrd->get_disposition()

Returns $self->{DISPOSITION}, the DISPOSITION of the PDRD.
This method should only be used on short form PDRDs.  
Use the get_files method to get the DISPOSITION of each file in long form PDRDs.
=back

=cut

#-----------------------------------------------------------------------------
sub get_disposition {
    my $self = shift;
    die "Invalid method for LONGPDRD MESSAGE_TYPE" 
        if $self->{MESSAGE_TYPE} eq "LONGPDRD";
    return $self->{DISPOSITION};
}

#-----------------------------------------------------------------------------

=over 4

=item $no_file_grps = $pdrd->get_no_file_grps()

Returns $self->{NO_FILE_GRPS}, the number of files in the PDRD.
This method should only be used on long form PDRDs.

=back

=cut

#-----------------------------------------------------------------------------
sub get_no_file_grps {
    my $self = shift;
    die "Invalid method for SHORTPDRD MESSAGE_TYPE" 
        if $self->{MESSAGE_TYPE} eq "SHORTPDRD";
    return $self->{NO_FILE_GRPS};
} #endsub

#-----------------------------------------------------------------------------

=over 4

=item @files = $pdrd->get_groups()

Gets an array of hashes with the DATA_TYPE and DISPOSITION for each group.  
This method should only be used on long form PDRDs.  The array of hashes has 
the following format:

=begin text

        @groups = 
        [
            {
                DATA_TYPE      => value, 
                DISPOSITION    => value, 
            },
            {
                DATA_TYPE      => value, 
                DISPOSITION    => value, 
            },
            etc.
        ];

=end text

=back

=cut

#-----------------------------------------------------------------------------
sub get_groups {
    my $self = shift;
    die "Invalid method for SHORTPDRD MESSAGE_TYPE" 
        if $self->{MESSAGE_TYPE} eq "SHORTPDRD";
    return @{ $self->{GROUPS} };
}

#-----------------------------------------------------------------------------

=head1 AUTHOR

Karen Horrocks, September 2000

=head1 ACKNOWLEDGMENTS

This software is developed by the MODAPS Team for the National Aeronautics and 
Space Administration, Goddard Space Flight Center, under contract NAS5-32373.

=cut

#-----------------------------------------------------------------------------
1;

